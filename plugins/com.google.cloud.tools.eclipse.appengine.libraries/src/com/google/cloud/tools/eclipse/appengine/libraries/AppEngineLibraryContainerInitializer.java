/*******************************************************************************
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
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.libraries;

import java.text.MessageFormat;
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

import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryFactory;
import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryFactory.LibraryFactoryException;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;

/**
 * {@link ClasspathContainerInitializer} implementation that resolves containers for App Engine libraries.
 * <p>
 * The container path is expected to be in the form of
 * &lt;value of {@link Library#CONTAINER_PATH_PREFIX}&gt;/&lt;library ID&gt;
 */
public class AppEngineLibraryContainerInitializer extends ClasspathContainerInitializer {

  public static final String LIBRARIES_EXTENSION_POINT = "com.google.cloud.tools.eclipse.appengine.libraries";

  private static final Logger logger = Logger.getLogger(AppEngineLibraryContainerInitializer.class.getName());

  private String containerPath = Library.CONTAINER_PATH_PREFIX;
  private Map<String, Library> libraries;

  public AppEngineLibraryContainerInitializer() {
    super();
  }

  @VisibleForTesting
  AppEngineLibraryContainerInitializer(IConfigurationElement[] configurationElements,
                                       LibraryFactory libraryFactory,
                                       String containerPath) {
    this.containerPath = containerPath;
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
                                                 MessageFormat.format("Unexpected first segment of container path, "
                                                                      + "expected: {0} was: {1}",
                                                                      this.containerPath,
                                                                      containerPath.segment(0))));
      }
      String libraryId = containerPath.lastSegment();
      Library library = libraries.get(libraryId);
      if (library != null) {
        LibraryClasspathContainer container = new LibraryClasspathContainer(containerPath, library);
        JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
                                       new IClasspathContainer[] {container}, null);
      } else {
        throw new CoreException(StatusUtil.error(this, "library not found for ID: " + libraryId));
      }
    } else {
      throw new CoreException(StatusUtil.error(this,
                                               "containerPath does not have exactly 2 segments: "
                                               + containerPath.toString()));
    }
  }

  private void initializeLibraries(IConfigurationElement[] configurationElements, LibraryFactory libraryFactory) {
      libraries = new HashMap<>(configurationElements.length);
      for (IConfigurationElement configurationElement : configurationElements) {
        try {
          Library library = libraryFactory.create(configurationElement);
          libraries.put(library.getId(), library);
        } catch (LibraryFactoryException exception) {
          logger.log(Level.SEVERE, "Failed to initialize libraries", exception);
        }
      }
  }
}
