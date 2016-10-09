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
import org.eclipse.core.runtime.Path;

import com.google.cloud.tools.eclipse.appengine.libraries.MavenCoordinates;

/**
 * Implementation of {@link ILibraryRepositoryService} that relies on M2Eclipse to download the artifacts and store
 * them in the local Maven repository pointed to by M2Eclipse's M2_REPO variable.
 */
// FIXME For now this class is just a mock, to be implemented soon
public class M2RepositoryService implements ILibraryRepositoryService {

  @Override
  public IPath getJarLocation(MavenCoordinates mavenCoordinates) {
    return new Path("/path/to/jar/file/in/m2_repo/" + mavenCoordinates.getArtifactId() + "." + mavenCoordinates.getType());
  }

  @Override
  public IPath getSourceJarLocation(MavenCoordinates mavenCoordinates) {
    return new Path("/path/to/source/jar/file/in/m2_repo/" + mavenCoordinates.getArtifactId() + "." + mavenCoordinates.getType());
  }

}
