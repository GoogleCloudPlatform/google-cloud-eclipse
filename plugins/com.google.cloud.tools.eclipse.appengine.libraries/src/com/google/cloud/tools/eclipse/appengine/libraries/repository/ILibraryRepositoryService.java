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
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Service interface for obtaining local paths for artifacts described by {@link MavenCoordinates}
 */
public interface ILibraryRepositoryService {

  /**
   * Creates a classpath entry with the kind {@link IClasspathEntry#CPE_LIBRARY} that refers to the artifact defined
   * by the <code>libraryFile</code> parameter.
   *
   * @return a classpath entry with reference to the artifact file resolved by this service and javadoc and source
   * attachment information if available
   * @throws LibraryRepositoryServiceException if the classpath entry cannot be created, e.g. cannot be resolved based
   * on the Maven coordinates or because of a network problem
   */
  IClasspathEntry getLibraryClasspathEntry(LibraryFile libraryFile) throws LibraryRepositoryServiceException;

}
