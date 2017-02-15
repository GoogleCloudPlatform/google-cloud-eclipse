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
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.NLS;

/**
 * {@link ClasspathContainerInitializer} implementation that resolves containers for App Engine libraries.
 * <p>
 * The container path is expected to be in the form of
 * &lt;value of {@link Library#CONTAINER_PATH_PREFIX}&gt;/&lt;library ID&gt;
 */
public class AppEngineLibraryContainerInitializer extends ClasspathContainerInitializer {

  public static final String LIBRARIES_EXTENSION_POINT = "com.google.cloud.tools.eclipse.appengine.libraries"; //$NON-NLS-1$

  private static final Logger logger = Logger.getLogger(AppEngineLibraryContainerInitializer.class.getName());

  private String containerPath = Library.CONTAINER_PATH_PREFIX;
  private Map<String, Library> libraries;

  private final LibraryClasspathContainerSerializer serializer;

  public AppEngineLibraryContainerInitializer() {
    serializer = new LibraryClasspathContainerSerializer();
  }

  @VisibleForTesting
  AppEngineLibraryContainerInitializer(IConfigurationElement[] configurationElements,
                                       LibraryFactory libraryFactory,
                                       String containerPath,
                                       LibraryClasspathContainerSerializer serializer) {
    this.containerPath = containerPath;
    this.serializer = serializer;
    initializeLibraries(configurationElements, libraryFactory);
  }

  @Override
  public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
    if (libraries == null) {
      // in tests libraries will be initialized via the test constructor, this would override mocks/stubs.
      IConfigurationElement[] configurationElements =
          RegistryFactory.getRegistry().getConfigurationElementsFor(LIBRARIES_EXTENSION_POINT);
      initializeLibraries(configurationElements, new LibraryFactory());
    }
    if (containerPath.segmentCount() == 2) {
      if (!containerPath.segment(0).equals(this.containerPath)) {
        throw new CoreException(StatusUtil.error(this,
                                                 NLS.bind(Messages.ContainerPathInvalidFirstSegment,
                                                          this.containerPath,
                                                          containerPath.segment(0))));
      }
      try {
        LibraryClasspathContainer container = serializer.loadContainer(project, containerPath);
        // TODO validate libraryFile paths and request resolution via ILibraryRepositoryService if they're invalid
        // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/855
        if (container != null) {
          JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
                                         new IClasspathContainer[] {container}, null);
        }
      } catch (IOException ex) {
        throw new CoreException(StatusUtil.error(this, Messages.LoadContainerFailed, ex));
      }
    } else {
      throw new CoreException(StatusUtil.error(this, NLS.bind(Messages.ContainerPathNotTwoSegments,
                                                              containerPath.toString())));
    }
  }

  // TODO parse library definition in ILibraryConfigService (or similar) started when the plugin/bundle starts
  // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/856
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
