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

import com.google.cloud.tools.eclipse.appengine.libraries.ILibraryClasspathContainerResolverService;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.jst.server.core.RuntimeClasspathProviderDelegate;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IRuntime;

/**
 * Supply Java servlet container classes, specifically servlet-api.jar, jsp-api.jar, and
 * appengine-api-1.0-sdk.jar to non-Maven projects.
 * <p>
 * The jars are resolved using {@link ILibraryRepositoryService}.
 */
public class ServletClasspathProvider extends RuntimeClasspathProviderDelegate {

  /**
   * The default Servlet API supported by the App Engine Java 7 runtime.
   */
  private static final IProjectFacetVersion DEFAULT_DYNAMIC_WEB_VERSION = WebFacetUtils.WEB_25;

  private static final IClasspathEntry[] NO_CLASSPATH_ENTRIES = {};

  private static final Logger logger = Logger.getLogger(ServletClasspathProvider.class.getName());

  @Inject
  private ILibraryClasspathContainerResolverService resolverService;

  @Override
  public IClasspathEntry[] resolveClasspathContainer(IProject project, IRuntime runtime) {
    if (project != null && MavenUtils.hasMavenNature(project)) { // Maven handles its own classpath
      return NO_CLASSPATH_ENTRIES;
    }

    // Runtime is expected to provide Servlet and JSP APIs
    IProjectFacetVersion webFacetVersion = DEFAULT_DYNAMIC_WEB_VERSION;
    try {
      IFacetedProject facetedProject = ProjectFacetsManager.create(project);
      webFacetVersion = facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET);
    } catch (CoreException ex) {
      logger.log(Level.WARNING, "Unable to obtain jst.web facet version", ex);
    }

    try {
      String[] apiLibraryIds = getApiLibraryIds(webFacetVersion);
      ListenableFuture<IClasspathEntry[]> apiEntries =
          resolverService.resolveLibraryAttachSources(apiLibraryIds);
      // may return immediately if cached or locally available
      if (apiEntries.isDone()) {
        return apiEntries.get();
      }
      Futures.addCallback(
          apiEntries,
          new FutureCallback<IClasspathEntry[]>() {
            @Override
            public void onSuccess(IClasspathEntry[] entries) {
              requestClasspathContainerUpdate(project, runtime, entries);
            }

            @Override
            public void onFailure(Throwable t) {
              logger.log(Level.WARNING, "Failed to resolve servlet APIs", t);
            }
          });
    } catch (CoreException | ExecutionException ex) {
      logger.log(Level.WARNING, "Failed to initialize libraries", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    return null;
  }

  /** Request that a project's Server Runtime classpath container be updated. */
  private void requestClasspathContainerUpdate(
      IProject project, IRuntime runtime, IClasspathEntry[] entries) {
    /*
     * The deceptively-named {@code requestClasspathContainerUpdate()} on our superclass
     * does not actually request an update of our Server Runtime classpath container.
     *
     * A JDT classpath container can be updated either by explicitly requesting an update from its
     * initializer ({@code ClasspathContainerInitializer#requestClasspathContainerUpdate()}), or
     * by calling {@code JavaCore.setClasspathContainer()}.  Both approaches require specifying the
     * container path (the container ID, so to speak), and the Server Runtime classpath container's
     * path is considered internal to WTP.
     *
     * But our superclass' {@code resolveClasspathContainerImpl()} implementation does call
     * {@code JavaCore.setClasspathContainer()} to update the container if the
     * classpath entries returned from our {@code resolveClasspathContainer()} change.
     * https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/3055#issuecomment-390242592
     */
    requestClasspathContainerUpdate(runtime, entries);
    resolveClasspathContainerImpl(project, runtime); // triggers update of this classpath container
  }

  /** Return the Library IDs for the Servlet APIs for the given dynamic web facet version. */
  private String[] getApiLibraryIds(IProjectFacetVersion dynamicWebVersion) {
    if (WebFacetUtils.WEB_31.equals(dynamicWebVersion)
        || WebFacetUtils.WEB_30.equals(dynamicWebVersion)) {
      return new String[] {"servlet-api-3.1", "jsp-api-2.3"};
    } else {
      return new String[] {"servlet-api-2.5", "jsp-api-2.1"};
    }
  }
}
