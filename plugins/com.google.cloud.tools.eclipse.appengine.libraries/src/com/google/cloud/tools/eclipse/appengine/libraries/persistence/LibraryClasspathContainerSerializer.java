/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.libraries.persistence;

import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainer;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.m2e.core.MavenPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Provides functionality to save and load {@link LibraryClasspathContainer} instances to disk.
 */
@Creatable
public class LibraryClasspathContainerSerializer {
  private static final Logger logger =
      Logger.getLogger(LibraryClasspathContainerSerializer.class.getName());

  private static final String CONTAINER_LIBRARY_LIST_FILE_ID = "_libraries"; //$NON-NLS-1$

  private final LibraryContainerStateLocationProvider stateLocationProvider;
  private final ArtifactBaseLocationProvider binaryArtifactBaseLocationProvider;
  private final ArtifactBaseLocationProvider sourceBaseLocationProvider;
  private final Gson gson;

  public LibraryClasspathContainerSerializer() {
    this(new DefaultStateLocationProvider(),
         new M2LocalRepositoryLocationProvider(),
         new LibrariesBundleStateLocationProvider());
  }

  @VisibleForTesting
  public LibraryClasspathContainerSerializer(
      LibraryContainerStateLocationProvider stateLocationProvider,
      ArtifactBaseLocationProvider binaryBaseLocationProvider,
      ArtifactBaseLocationProvider sourceBaseLocationProvider) {
    this.stateLocationProvider = stateLocationProvider;
    this.binaryArtifactBaseLocationProvider = binaryBaseLocationProvider;
    this.sourceBaseLocationProvider = sourceBaseLocationProvider;
    gson = new GsonBuilder().setPrettyPrinting().create();
  }

  public void saveContainer(IJavaProject javaProject, LibraryClasspathContainer container)
      throws IOException, CoreException {
    File stateFile = getContainerStateFile(javaProject, container.getPath().lastSegment(), true);
    if (stateFile == null) {
      logger.warning("Container state file cannot be created, save failed"); //$NON-NLS-1$
      return;
    }
    try (Writer out = Files.newBufferedWriter(stateFile.toPath(), StandardCharsets.UTF_8)) {
      SerializableLibraryClasspathContainer serializableContainer =
          new SerializableLibraryClasspathContainer(container,
              binaryArtifactBaseLocationProvider.getBaseLocation(),
              sourceBaseLocationProvider.getBaseLocation());
      out.write(gson.toJson(serializableContainer));
    }
  }

  public LibraryClasspathContainer loadContainer(IJavaProject javaProject, IPath containerPath)
      throws IOException, CoreException {
    File stateFile = getContainerStateFile(javaProject, containerPath.lastSegment(), false);
    if (stateFile == null) {
      return null;
    }
    try (Reader reader =
        Files.newBufferedReader(stateFile.toPath(), StandardCharsets.UTF_8)) {
      SerializableLibraryClasspathContainer fromJson =
          gson.fromJson(reader, SerializableLibraryClasspathContainer.class);
      if (fromJson == null) {
        return null;
      }
      
      LibraryClasspathContainer libraryClasspathContainer = fromJson.toLibraryClasspathContainer(
          javaProject,
          binaryArtifactBaseLocationProvider.getBaseLocation(),
          sourceBaseLocationProvider.getBaseLocation());
      return libraryClasspathContainer;
    }
  }

  public void resetContainer(IJavaProject javaProject, IPath containerPath)
      throws CoreException {
    // delete the container state cache file since the library list has changed
    stateLocationProvider.removeContainerStateFile(javaProject, containerPath.lastSegment());
  }

  /**
   * Persist the set of library IDs for this project; callers will likely need to rebuild the
   * container state cache file.
   */
  public void saveLibraryIds(IJavaProject javaProject, List<String> libraryIds)
      throws CoreException, IOException {
    File librariesFile = getContainerStateFile(javaProject, CONTAINER_LIBRARY_LIST_FILE_ID, true);
    if (librariesFile == null) {
      logger.warning("Library-id state file cannot be created, save failed"); //$NON-NLS-1$
      return;
    }
    try (Writer out = Files.newBufferedWriter(librariesFile.toPath(), StandardCharsets.UTF_8)) {
      out.write(gson.toJson(libraryIds.toArray()));
    }
  }

  public List<String> loadLibraryIds(IJavaProject javaProject)
      throws IOException, CoreException {
    File stateFile = getContainerStateFile(javaProject, CONTAINER_LIBRARY_LIST_FILE_ID, false);
    if (stateFile == null) {
      return Collections.emptyList();
    }
    try (Reader reader = Files.newBufferedReader(stateFile.toPath(), StandardCharsets.UTF_8)) {
      JsonArray array = gson.fromJson(reader, JsonArray.class);
      if (array == null) {
        return Collections.emptyList();
      }
      List<String> libraryIds = new ArrayList<>(array.size());
      for (JsonElement element : array) {
        libraryIds.add(element.getAsString());
      }
      return libraryIds;
    } catch (JsonSyntaxException ex) {
      logger.log(Level.WARNING, "Invalid content in library-id state file: " + stateFile, ex); //$NON-NLS-1$
      return Collections.emptyList();
    }
  }

  /** Return {@code true} if the project has a list of selected library IDs. */
  public boolean hasLibraryIds(IJavaProject javaProject) {
    try {
      File stateFile = getContainerStateFile(javaProject, CONTAINER_LIBRARY_LIST_FILE_ID, false);
      return stateFile != null;
    } catch (CoreException ex) {
      return false;
    }
  }

  public void removeLibraryIds(IJavaProject javaProject) throws CoreException {
    stateLocationProvider.removeContainerStateFile(javaProject, CONTAINER_LIBRARY_LIST_FILE_ID);
  }

  private File getContainerStateFile(IJavaProject javaProject, String fileId, boolean create)
      throws CoreException {
    IPath containerStateFile =
        stateLocationProvider.getContainerStateFile(javaProject, fileId, create);
    if (containerStateFile != null && containerStateFile.toFile().exists()) {
      return containerStateFile.toFile();
    }
    return null;
  }


  private static class DefaultStateLocationProvider
      implements LibraryContainerStateLocationProvider {

    /*
     * The IFile and IFolder methods do not validate whether the underlying resources exist.
     * Therefore if <code>create</code> is false, they will not fail or throw and error.
     */
    @Override
    public IPath getContainerStateFile(IJavaProject javaProject, String id,
        boolean create) throws CoreException {
      IFile containerFile = getFile(javaProject, id, create);
      if (!containerFile.exists() && create) {
        containerFile.create(new ByteArrayInputStream(new byte[0]), true, null);
      }
      return containerFile.getLocation();
    }

    private IFile getFile(IJavaProject javaProject, String id, boolean create)
        throws CoreException {
      IFolder settingsFolder = javaProject.getProject().getFolder(".settings"); //$NON-NLS-1$
      IFolder folder =
          settingsFolder.getFolder(FrameworkUtil.getBundle(getClass()).getSymbolicName());
      if (!folder.exists() && create) {
        ResourceUtils.createFolders(folder, null);
      }
      IFile containerFile = folder.getFile(id + ".container"); //$NON-NLS-1$
      return containerFile;
    }

    @Override
    public void removeContainerStateFile(IJavaProject javaProject, String id) throws CoreException {
      IFile stateFile = getFile(javaProject, id, false);
      Verify.verifyNotNull(stateFile);
      if (stateFile.exists()) {
        stateFile.delete(true, new NullProgressMonitor());
      }
    }
  }

  private static class M2LocalRepositoryLocationProvider implements ArtifactBaseLocationProvider {

    /**
     * @see com.google.cloud.tools.eclipse.appengine.libraries.persistence.ArtifactBaseLocationProvider#getBaseLocation()
     */
    @Override
    public IPath getBaseLocation() {
      return new Path(
          MavenPlugin.getRepositoryRegistry().getLocalRepository().getBasedir().getAbsolutePath());
    }
  }

  private static class LibrariesBundleStateLocationProvider
      implements ArtifactBaseLocationProvider {

    private static final String APPENGINE_LIBRARIES_BUNDLE_NAME =
        "com.google.cloud.tools.eclipse.appengine.libraries"; //$NON-NLS-1$

    /**
     * @see com.google.cloud.tools.eclipse.appengine.libraries.persistence.ArtifactBaseLocationProvider#getBaseLocation()
     */
    @Override
    public IPath getBaseLocation() {
      Bundle librariesBundle = Platform.getBundle(APPENGINE_LIBRARIES_BUNDLE_NAME);
      Preconditions.checkState(librariesBundle != null,
          "Bundle Cloud Tools For Eclipse App Engine Libraries Management not found"); //$NON-NLS-1$
      return Platform.getStateLocation(librariesBundle);
    }
  }
}
