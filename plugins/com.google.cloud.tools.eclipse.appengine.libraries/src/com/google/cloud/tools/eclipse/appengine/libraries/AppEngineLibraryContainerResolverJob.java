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

package com.google.cloud.tools.eclipse.appengine.libraries;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFactory;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFactoryException;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.LibraryRepositoryServiceException;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.NLS;

public class AppEngineLibraryContainerResolverJob extends Job {
  //TODO duplicate of com.google.cloud.tools.eclipse.appengine.libraries.AppEngineLibraryContainerInitializer.LIBRARIES_EXTENSION_POINT
  public static final String LIBRARIES_EXTENSION_POINT = "com.google.cloud.tools.eclipse.appengine.libraries"; //$NON-NLS-1$

  private static final Logger logger = Logger.getLogger(AppEngineLibraryContainerResolverJob.class.getName());

  private Map<String, Library> libraries;

  @Inject
  private IJavaProject javaProject;
  @Inject
  private ILibraryRepositoryService repositoryService;
  @Inject
  private LibraryClasspathContainerSerializer serializer;

  public AppEngineLibraryContainerResolverJob() {
    super(Messages.AppEngineLibraryContainerResolverJobName);
    setUser(true);
  }

  @VisibleForTesting
  AppEngineLibraryContainerResolverJob(LibraryClasspathContainerSerializer serializer) {
    super(Messages.AppEngineLibraryContainerResolverJobName);
    Preconditions.checkNotNull(serializer);
    this.serializer = serializer;
    setUser(true);
  }

  @PostConstruct
  public void init() {
    setRule(javaProject.getSchedulingRule());
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    // TODO parse library definition in ILibraryConfigService (or similar) started when the plugin/bundle starts
    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/856
    try {
      if (libraries == null) {
        // in tests libraries will be initialized via the test constructor, this would override mocks/stubs.
        IConfigurationElement[] configurationElements =
            RegistryFactory.getRegistry().getConfigurationElementsFor(LIBRARIES_EXTENSION_POINT);
        initializeLibraries(configurationElements, new LibraryFactory());
      }
      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      SubMonitor subMonitor = SubMonitor.convert(monitor,
                                                 Messages.TaskResolveLibraries,
                                                 getTotalwork(rawClasspath));
      for (int i = 0; i < rawClasspath.length; i++) {
        IClasspathEntry classpathEntry = rawClasspath[i];
        String libraryId = classpathEntry.getPath().segment(1);
        Library library = libraries.get(libraryId);
        if (library != null) {
          LibraryClasspathContainer container = resolveLibraryFiles(classpathEntry, library, subMonitor.newChild(1));
          JavaCore.setClasspathContainer(classpathEntry.getPath(), new IJavaProject[] {javaProject},
                                         new IClasspathContainer[] {container}, null);
          serializer.saveContainer(javaProject, container);
        }
      }
    } catch (LibraryRepositoryServiceException | CoreException | IOException ex) {
      return StatusUtil.error(this, Messages.TaskResolveLibrariesError, ex);
    }
    return Status.OK_STATUS;
  }

  private LibraryClasspathContainer resolveLibraryFiles(IClasspathEntry classpathEntry,
                                                        Library library,
                                                        IProgressMonitor monitor)
                                                            throws LibraryRepositoryServiceException {
    List<LibraryFile> libraryFiles = library.getLibraryFiles();
    SubMonitor subMonitor = SubMonitor.convert(monitor, libraryFiles.size());
    subMonitor.subTask(NLS.bind(Messages.TaskResolveArtifacts, getLibraryDescription(library)));
    SubMonitor child = subMonitor.newChild(libraryFiles.size());

    IClasspathEntry[] entries = new IClasspathEntry[libraryFiles.size()];
    int idx = 0;
    for (LibraryFile libraryFile : libraryFiles) {
      entries[idx++] = repositoryService.getLibraryClasspathEntry(libraryFile);
      child.worked(1);
    }
    monitor.done();
    LibraryClasspathContainer container = new LibraryClasspathContainer(classpathEntry.getPath(),
                                                                        getLibraryDescription(library),
                                                                        entries);
    return container;
  }

  private static int getTotalwork(IClasspathEntry[] rawClasspath) {
    int sum = 0;
    for (IClasspathEntry element : rawClasspath) {
      if (isLibraryClasspathEntry(element.getPath())) {
        ++sum;
      }
    }
    return sum;
  }

  private static boolean isLibraryClasspathEntry(IPath path) {
    return path != null && path.segmentCount() == 2 && Library.CONTAINER_PATH_PREFIX.equals(path.segment(0));
  }

  private static String getLibraryDescription(Library library) {
    if (!Strings.isNullOrEmpty(library.getName())) {
      return library.getName();
    } else {
      return library.getId();
    }
  }

  private void initializeLibraries(IConfigurationElement[] configurationElements, LibraryFactory libraryFactory) {
    libraries = new HashMap<>(configurationElements.length);
    for (IConfigurationElement configurationElement : configurationElements) {
      try {
        Library library = libraryFactory.create(configurationElement);
        libraries.put(library.getId(), library);
      } catch (LibraryFactoryException exception) {
        logger.log(Level.SEVERE, "Failed to initialize libraries", exception); //$NON-NLS-1$
      }
    }
  }
}
