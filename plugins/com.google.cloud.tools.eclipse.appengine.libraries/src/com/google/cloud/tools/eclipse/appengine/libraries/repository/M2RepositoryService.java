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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.MavenPlugin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.google.cloud.tools.eclipse.appengine.libraries.MavenCoordinates;
import com.google.cloud.tools.eclipse.util.MavenUtils;

/**
 * Implementation of {@link ILibraryRepositoryService} that relies on M2Eclipse to download the artifacts and store
 * them in the local Maven repository pointed to by M2Eclipse's M2_REPO variable.
 */
@Component
public class M2RepositoryService implements ILibraryRepositoryService {

  @Override
  public IPath getJarLocation(MavenCoordinates mavenCoordinates) throws LibraryRepositoryServiceException {
    try {
      List<ArtifactRepository> repository = getRepository(mavenCoordinates);

      Artifact artifact;
      if (mavenCoordinates.getVersion().equals(MavenCoordinates.LATEST_VERSION)) {
        artifact = MavenUtils.resolveLatestReleasedArtifact(null, mavenCoordinates.getGroupId(), mavenCoordinates.getArtifactId(), mavenCoordinates.getType(), mavenCoordinates.getClassifier(), repository);
      } else {
        artifact = MavenUtils.resolveArtifact(null, mavenCoordinates.getGroupId(), mavenCoordinates.getArtifactId(), mavenCoordinates.getType(), mavenCoordinates.getVersion(), mavenCoordinates.getClassifier(), repository);
      }

      return new Path(artifact.getFile().getAbsolutePath());
    } catch (CoreException ex) {
      throw new LibraryRepositoryServiceException("Could not resolve maven artifact: " + mavenCoordinates, ex);
    }
  }

  private ArtifactRepository getRepository(String repository) throws LibraryRepositoryServiceException {
    try {
      URI repoUri = new URI(repository);
      if (!repoUri.isAbsolute()) {
        throw new LibraryRepositoryServiceException("repository URI must be an absolute URI (i.e. has to have a "
            + "schema component)");
      }
      return MavenPlugin.getMaven().createArtifactRepository(repoUri.getHost(), repoUri.toString());
    } catch (URISyntaxException exception) {
      throw new LibraryRepositoryServiceException("repository is not a valid URI and currenlty only 'central' is "
          + "supported as repository ID",
          exception);
    } catch (CoreException exception) {
      throw new LibraryRepositoryServiceException("Could not create remote repository: " + repository, exception);
    }
  }

  private List<ArtifactRepository> getRepository(MavenCoordinates mavenCoordinates) throws LibraryRepositoryServiceException {
    if (mavenCoordinates.getRepository().equals(MavenCoordinates.MAVEN_CENTRAL_REPO)) {
      return null;
    } else {
      return Collections.singletonList(getRepository(mavenCoordinates.getRepository()));
    }
  }

  @Override
  public IPath getSourceJarLocation(MavenCoordinates mavenCoordinates) {
    return new Path("/path/to/source/jar/file/in/m2_repo/" + mavenCoordinates.getArtifactId() + "." + mavenCoordinates.getType());
  }

  @Activate
  protected void activate() throws LibraryRepositoryServiceException {
  }
}
