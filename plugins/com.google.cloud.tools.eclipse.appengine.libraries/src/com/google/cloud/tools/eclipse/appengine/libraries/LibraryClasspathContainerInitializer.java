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
import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import javax.inject.Inject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.NLS;

/**
 * {@link ClasspathContainerInitializer} implementation that resolves containers for App Engine
 * libraries.
 * <p>
 * The container path is expected to be in the form of
 * &lt;value of {@link Library#CONTAINER_PATH_PREFIX}&gt;/&lt;library ID&gt;
 */
public class LibraryClasspathContainerInitializer extends ClasspathContainerInitializer {

  @Inject
  private LibraryClasspathContainerSerializer serializer;
  @Inject
  private ILibraryClasspathContainerResolverService resolverService;

  public LibraryClasspathContainerInitializer() {
  }

  @VisibleForTesting
  LibraryClasspathContainerInitializer(LibraryClasspathContainerSerializer serializer,
                                       ILibraryClasspathContainerResolverService resolverService) {
    this.serializer = serializer;
    this.resolverService = resolverService;
  }

  @Override
  public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
    if (containerPath.segmentCount() != 2) {
      throw new CoreException(StatusUtil.error(this, NLS.bind(Messages.ContainerPathNotTwoSegments,
                                                              containerPath.toString())));
    }
    if (!containerPath.segment(0).equals(Library.CONTAINER_PATH_PREFIX)) {
      throw new CoreException(StatusUtil.error(this,
                                               NLS.bind(Messages.ContainerPathInvalidFirstSegment,
                                                        Library.CONTAINER_PATH_PREFIX,
                                                        containerPath.segment(0))));
    }
    try {
      LibraryClasspathContainer container = serializer.loadContainer(project, containerPath);
      if (container != null && jarPathsAreValid(container)) {
        JavaCore.setClasspathContainer(containerPath,
                                       new IJavaProject[] {project},
                                       new IClasspathContainer[] {container},
                                       new NullProgressMonitor());
      } else {
        resolverService.resolveContainer(project, containerPath, new NullProgressMonitor());
      }
    } catch (IOException ex) {
      throw new CoreException(StatusUtil.error(this, Messages.LoadContainerFailed, ex));
    }
  }

  private static boolean jarPathsAreValid(LibraryClasspathContainer container) {
    IClasspathEntry[] classpathEntries = container.getClasspathEntries();
    for (int i = 0; i < classpathEntries.length; i++) {
      IClasspathEntry classpathEntry = classpathEntries[i];
      if (!classpathEntry.getPath().toFile().exists()
          || (classpathEntry.getSourceAttachmentPath() != null
          && !classpathEntry.getSourceAttachmentPath().toFile().exists())) {
        return false;
      }
    }
    return true;
  }
}
