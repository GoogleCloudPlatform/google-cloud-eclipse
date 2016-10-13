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
package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import org.eclipse.core.runtime.IPath;

import com.google.cloud.tools.eclipse.appengine.libraries.MavenCoordinates;

/**
 * Service interface for obtaining local paths for artifacts described by {@link MavenCoordinates}
 */
public interface ILibraryRepositoryService {

  /**
   * @return a path that points to a local file corresponding to the artifact described by <code>mavenCoordinates</code>
   * @throws LibraryRepositoryServiceException if the resolution of the artifact defined by
   * <code>mavenCoordinates</code> fails.
   */
  IPath getJarLocation(MavenCoordinates mavenCoordinates) throws LibraryRepositoryServiceException;

  /**
   * @return a path that points to a local file corresponding to the source artifact described
   * by <code>mavenCoordinates</code>
   */
  IPath getSourceJarLocation(MavenCoordinates mavenCoordinates);

  /**
   * @return a path that points to a local file corresponding to the javadoc artifact described
   * by <code>mavenCoordinates</code>
   */
  IPath getJavadocJarLocation(MavenCoordinates mavenCoordinates);

}
