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
import java.text.MessageFormat;
import javax.inject.Inject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

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
  private String containerPathPrefix = Library.CONTAINER_PATH_PREFIX;

  public LibraryClasspathContainerInitializer() {
  }

  @VisibleForTesting
  LibraryClasspathContainerInitializer(String containerPathPrefix,
                                       LibraryClasspathContainerSerializer serializer,
                                       ILibraryClasspathContainerResolverService resolverService) {
    this.containerPathPrefix = containerPathPrefix;
    this.serializer = serializer;
    this.resolverService = resolverService;
  }

  @Override
  public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
    if (containerPath.segmentCount() != 2) {
      throw new CoreException(StatusUtil.error(this,
                                               "containerPath does not have exactly 2 segments: "
                                                   + containerPath.toString()));
    }
    if (!containerPath.segment(0).equals(containerPathPrefix)) {
      throw new CoreException(
          StatusUtil.error(this, MessageFormat.format("Unexpected first segment of container path, "
                                                          + "expected: {0} was: {1}",
                                                      containerPathPrefix,
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
      throw new CoreException(StatusUtil.error(this,
                                               "Failed to load persisted container descriptor",
                                               ex));
    }
  }

  private static boolean jarPathsAreValid(LibraryClasspathContainer container) {
    IClasspathEntry[] classpathEntries = container.getClasspathEntries();
    for (IClasspathEntry classpathEntry : classpathEntries) {
      if (!classpathEntry.getPath().toFile().exists()
          || (classpathEntry.getSourceAttachmentPath() != null
          && !classpathEntry.getSourceAttachmentPath().toFile().exists())) {
        return false;
      }
    }
    return true;
  }
}
