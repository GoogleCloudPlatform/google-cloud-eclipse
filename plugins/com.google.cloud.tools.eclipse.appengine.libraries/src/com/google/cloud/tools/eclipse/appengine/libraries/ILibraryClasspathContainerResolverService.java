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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Service interface to resolve {@link LibraryClasspathContainer}s.
 */
public interface ILibraryClasspathContainerResolverService {

  enum AppEngineRuntime {
    STANDARD_JAVA_7
  }

  /**
   * Resolves all {@link LibraryClasspathContainer}s found on the classpath of
   * <code>javaProject</code>. Source attachment for the resolved libraries will happen
   * asynchronously.
   */
  IStatus resolveAll(IJavaProject javaProject, IProgressMonitor monitor);

  /**
   * Resolves the binary and source artifacts corresponding to the {@link Library} identified by
   * <code>libraryId</code> synchronously and creates the {@link IClasspathEntry}s referring them.
   */
  IClasspathEntry[] resolveLibraryAttachSourcesSync(String libraryId) throws CoreException;

  /**
   * Resolves a single {@link LibraryClasspathContainer} corresponding to <code>containerPath</code>
   * in <code>javaProject</code>. Sources for the resolved binary artifacts are resolved
   * asynchronously.
   */
  IStatus resolveContainer(IJavaProject javaProject, IPath containerPath, IProgressMonitor monitor);


  /**
   * Verifies that dependencies of a given runtime are available locally or can be downloaded.
   *
   * @return {@link Status#OK_STATUS} if all dependencies are available,
   * {@link Status#CANCEL_STATUS} if the operation was cancelled via the <code>monitor</code>, or a
   * {@link Status} object describing the error that happened while checking availability
   */
  IStatus checkRuntimeAvailability(AppEngineRuntime runtime, IProgressMonitor monitor);
}