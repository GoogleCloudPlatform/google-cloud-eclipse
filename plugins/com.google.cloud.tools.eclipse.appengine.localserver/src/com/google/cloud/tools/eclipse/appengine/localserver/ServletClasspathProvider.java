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

package com.google.cloud.tools.eclipse.appengine.localserver;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFactory;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFactoryException;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.LibraryRepositoryServiceException;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jst.server.core.RuntimeClasspathProviderDelegate;
import org.eclipse.wst.server.core.IRuntime;

/**
 * Supply Java standard classes, specifically servlet-api.jar and jsp-api.jar,
 * to non-Maven projects.
 * <p>
 * The jars are resolved using {@link ILibraryRepositoryService}.
 */
public class ServletClasspathProvider extends RuntimeClasspathProviderDelegate {

  private static final Logger logger = Logger.getLogger(ServletClasspathProvider.class.getName());

  private Map<String, Library> libraries;

  @Inject
  private ILibraryRepositoryService service;
  @Inject
  private IExtensionRegistry extensionRegistry;

  public ServletClasspathProvider() {
  }

  @VisibleForTesting
  ServletClasspathProvider(Map<String, Library> libraries, ILibraryRepositoryService repositoryService) {
    this.libraries = libraries;
    service = repositoryService;
  }

  @Override
  public IClasspathEntry[] resolveClasspathContainer(IProject project, IRuntime runtime) {
    if (project != null && MavenUtils.hasMavenNature(project)) { // Maven handles its own classpath
      return null;
    } else {
      return resolveClasspathContainer(runtime);
    }
  }

  @Override
  public IClasspathEntry[] resolveClasspathContainer(IRuntime runtime) {
    try {
      initializeLibraries(new LibraryFactory());

      // servlet api is assumed to be a single file
      List<LibraryFile> servletApiLibraryFiles = libraries.get("servlet-api").getLibraryFiles();
      Preconditions.checkState(servletApiLibraryFiles.size() == 1);
      LibraryFile servletApi = servletApiLibraryFiles.get(0);
      IClasspathEntry servletApiEntry = service.getLibraryClasspathEntry(servletApi);

      // jsp api is assumed to be a single file
      List<LibraryFile> jspApiLibraryFiles = libraries.get("jsp-api").getLibraryFiles();
      Preconditions.checkState(jspApiLibraryFiles.size() == 1);
      LibraryFile jspApi = jspApiLibraryFiles.get(0);
      IClasspathEntry jspApiEntry = service.getLibraryClasspathEntry(jspApi);

      return new IClasspathEntry[] { servletApiEntry, jspApiEntry };
    } catch (LibraryRepositoryServiceException e) {
      return null;
    }
  }

  // TODO parse library definition in ILibraryConfigService (or similar) started when the plugin/bundle starts
  // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/856
  private void initializeLibraries(LibraryFactory libraryFactory) {
    if (libraries == null) {
      IConfigurationElement[] configurationElements =
          extensionRegistry.getConfigurationElementsFor("com.google.cloud.tools.eclipse.appengine.libraries");
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
}
