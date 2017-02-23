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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;

import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainer;

/**
 * Implementers of this interface provide a location of a file that can be used to save and load
 * {@link LibraryClasspathContainer}s
 */
public interface LibraryContainerStateLocationProvider {

  /**
   * Based on the parameters <code>javaProject</code> and <code>containerPath</code> will return
   * an {@link IPath} to a file that can be used to save and load
   * {@link LibraryClasspathContainer} instances.
   *
   * @param javaProject the project the {@link LibraryClasspathContainer} belongs to
   * @param containerPath the container path of the {@link LibraryClasspathContainer}
   * @param create if true the file and parent folders will be created if needed; if false the
   *        location returned may not refer to an existing file.
   *
   * @throws CoreException if an error happens while creating the necessary folders or file
   */
  IPath getContainerStateFile(IJavaProject javaProject, IPath containerPath, boolean create)
      throws CoreException;
}