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

package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Service interface for obtaining local paths for artifacts described by {@link MavenCoordinates}
 */
public interface ILibraryRepositoryService {

  /**
   * @return an <code>artifact</code> that is described by <code>libraryFile</code>
   */
  Artifact resolveArtifact(LibraryFile libraryFile, IProgressMonitor monitor) throws CoreException;

  /**
   * Resolves a source artifact for the binary artifact described by a {@link LibraryFile}.
   * <p>
   * If <code>libraryFile.getSourceUri()</code> is not null, the source will be downloaded from that
   * URI, otherwise resolving a suitable source artifact is up to the implementation (e.g. using
   * Maven/M2E).
   *
   * @param libraryFile the artifact whose source artifact needs to be resolved
   * @param versionHint the actual version to be resolved in case <code>libraryFile</code>'s version
   *     is set to latest
   * @return a path of the resolved source artifact
   */
  IPath resolveSourceArtifact(LibraryFile libraryFile, String versionHint, IProgressMonitor monitor)
      throws CoreException;

  /**
   * Checks if an artifact described by <code>libraryFile</code> is available. Throws a
   * {@link CoreException} if the artifact is not available.
   */
  void makeArtifactAvailable(LibraryFile libraryFile, IProgressMonitor monitor)
      throws CoreException;
}
