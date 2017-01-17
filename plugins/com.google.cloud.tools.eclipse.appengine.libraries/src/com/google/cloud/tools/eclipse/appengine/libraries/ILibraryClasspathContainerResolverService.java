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

import com.google.cloud.tools.eclipse.appengine.libraries.repository.LibraryRepositoryServiceException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

public interface ILibraryClasspathContainerResolverService {

  public static final String LIBRARIES_EXTENSION_POINT =
      "com.google.cloud.tools.eclipse.appengine.libraries"; //$NON-NLS-1$

  public IStatus resolveAll(IJavaProject javaProject, IProgressMonitor monitor);

  public IClasspathEntry[] resolveLibraryAttachSourcesSync(String libraryId)
                                                               throws CoreException,
                                                                      LibraryRepositoryServiceException;

  public IStatus resolveContainer(IJavaProject javaProject,
                                  IPath continerPath,
                                  IProgressMonitor monitor);
}