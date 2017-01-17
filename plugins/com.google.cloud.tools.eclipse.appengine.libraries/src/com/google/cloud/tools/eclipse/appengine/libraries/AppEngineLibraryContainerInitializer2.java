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
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.osgi.util.NLS;

/**
 * {@link ClasspathContainerInitializer} implementation that resolves containers for App Engine libraries.
 * <p>
 * The container path is expected to be in the form of
 * &lt;value of {@link Library#CONTAINER_PATH_PREFIX}&gt;/&lt;library ID&gt;
 */
public class AppEngineLibraryContainerInitializer2 extends ClasspathContainerInitializer {

  private String containerPath = Library.CONTAINER_PATH_PREFIX;

  public AppEngineLibraryContainerInitializer2() {
  }

  @VisibleForTesting
  AppEngineLibraryContainerInitializer2(IConfigurationElement[] configurationElements,
                                       LibraryFactory libraryFactory,
                                       String containerPath,
                                       LibraryClasspathContainerSerializer serializer) {
    this(configurationElements, libraryFactory, containerPath, serializer, null);
  }

  @VisibleForTesting
  AppEngineLibraryContainerInitializer2(IConfigurationElement[] configurationElements,
                                       LibraryFactory libraryFactory,
                                       String containerPath,
                                       LibraryClasspathContainerSerializer serializer,
                                       ILibraryRepositoryService repositoryService) {
    this.containerPath = containerPath;
  }

  @Override
  public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
    if (containerPath.segmentCount() == 2) {
      if (!containerPath.segment(0).equals(this.containerPath)) {
        throw new CoreException(StatusUtil.error(this,
                                                 NLS.bind(Messages.ContainerPathInvalidFirstSegment,
                                                          this.containerPath,
                                                          containerPath.segment(0))));
      }
      new AppEngineLibraryContainerResolver(project).resolveContainer(containerPath, new NullProgressMonitor());
    } else {
      throw new CoreException(StatusUtil.error(this, NLS.bind(Messages.ContainerPathNotTwoSegments,
                                                              containerPath.toString())));
    }
  }
}
