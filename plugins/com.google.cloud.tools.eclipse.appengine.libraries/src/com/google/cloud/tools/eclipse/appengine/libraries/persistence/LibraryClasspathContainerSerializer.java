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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
    File stateFile = getContainerStateFile(javaProject, container.getPath(), true);
    if (stateFile == null) {
      logger.warning("Container state file cannot be created, save failed");
      return;
    }
    try (OutputStreamWriter out =
        new OutputStreamWriter(new FileOutputStream(stateFile), StandardCharsets.UTF_8)) {
      SerializableLibraryClasspathContainer serializableContainer =
          new SerializableLibraryClasspathContainer(container,
              binaryArtifactBaseLocationProvider.getBaseLocation(),
              sourceBaseLocationProvider.getBaseLocation());
      out.write(gson.toJson(serializableContainer));
    }
  }

  public LibraryClasspathContainer loadContainer(IJavaProject javaProject, IPath containerPath)
      throws IOException, CoreException {
    File stateFile = getContainerStateFile(javaProject, containerPath, false);
    if (stateFile == null) {
      return null;
    }
    try (Reader reader =
        new InputStreamReader(new FileInputStream(stateFile), StandardCharsets.UTF_8)) {
      SerializableLibraryClasspathContainer fromJson =
          gson.fromJson(reader, SerializableLibraryClasspathContainer.class);
      if (fromJson == null) {
        return null;
      }
      return fromJson.toLibraryClasspathContainer(
          binaryArtifactBaseLocationProvider.getBaseLocation(),
          sourceBaseLocationProvider.getBaseLocation());
    }
  }

  private File getContainerStateFile(IJavaProject javaProject, IPath containerPath, boolean create)
      throws CoreException {
    IPath containerStateFile = stateLocationProvider.getContainerStateFile(javaProject,
                                                                           containerPath,
                                                                           create);
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
    public IPath getContainerStateFile(IJavaProject javaProject, IPath containerPath,
        boolean create) throws CoreException {
      IFolder settingsFolder = javaProject.getProject().getFolder(".settings");
      IFolder folder =
          settingsFolder.getFolder(FrameworkUtil.getBundle(getClass()).getSymbolicName());
      if (!folder.exists() && create) {
        folder.create(true, true, null);
      }
      IFile containerFile = folder.getFile(containerPath.segment(1) + ".container");
      if (!containerFile.exists() && create) {
        containerFile.create(new ByteArrayInputStream(new byte[0]), true, null);
      }
      return containerFile.getLocation();
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
        "com.google.cloud.tools.eclipse.appengine.libraries";

    /**
     * @see com.google.cloud.tools.eclipse.appengine.libraries.persistence.ArtifactBaseLocationProvider#getBaseLocation()
     */
    @Override
    public IPath getBaseLocation() {
      Bundle librariesBundle = Platform.getBundle(APPENGINE_LIBRARIES_BUNDLE_NAME);
      Preconditions.checkState(librariesBundle != null,
          "Bundle Cloud Tools For Eclipse App Engine Libraries Management not found");
      return Platform.getStateLocation(librariesBundle);
    }
  }
}
