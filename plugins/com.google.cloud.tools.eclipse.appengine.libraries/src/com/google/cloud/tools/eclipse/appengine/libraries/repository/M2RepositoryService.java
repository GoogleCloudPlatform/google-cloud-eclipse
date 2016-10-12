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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.google.cloud.tools.eclipse.appengine.libraries.MavenCoordinates;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * Implementation of {@link ILibraryRepositoryService} that relies on M2Eclipse to download the artifacts and store
 * them in the local Maven repository pointed to by M2Eclipse's M2_REPO variable.
 */
@Component
public class M2RepositoryService implements ILibraryRepositoryService {

  private MavenHelper mavenHelper;

  @Override
  public IPath getJarLocation(MavenCoordinates mavenCoordinates) throws LibraryRepositoryServiceException {
    Preconditions.checkState(mavenHelper != null, "mavenHelper is null");
    try {
      List<ArtifactRepository> repository = getRepository(mavenCoordinates);

      Artifact artifact = mavenHelper.resolveArtifact(null, mavenCoordinates, repository);

      return new Path(artifact.getFile().getAbsolutePath());
    } catch (CoreException ex) {
      throw new LibraryRepositoryServiceException("Could not resolve maven artifact: " + mavenCoordinates, ex);
    }
  }

  private ArtifactRepository getCustomRepository(String repository) throws LibraryRepositoryServiceException {
    try {
      URI repoUri = new URI(repository);
      if (!repoUri.isAbsolute()) {
        throw new LibraryRepositoryServiceException("repository URI must be an absolute URI (i.e. has to have a "
            + "schema component): " + repository);
      }
      return mavenHelper.createArtifactRepository(repoUri.getHost(), repoUri.toString());
    } catch (URISyntaxException exception) {
      throw new LibraryRepositoryServiceException("repository is not a valid URI and currently only 'central' is "
          + "supported as repository ID: " + repository,
          exception);
    } catch (CoreException exception) {
      throw new LibraryRepositoryServiceException("Could not locate remote repository: " + repository, exception);
    }
  }

  private List<ArtifactRepository> getRepository(MavenCoordinates mavenCoordinates) throws LibraryRepositoryServiceException {
    if (MavenCoordinates.MAVEN_CENTRAL_REPO.equals(mavenCoordinates.getRepository())) {
      // M2Eclipse will use the Maven Central repo in case null is used
      return null;
    } else {
      return Collections.singletonList(getCustomRepository(mavenCoordinates.getRepository()));
    }
  }

  @Override
  public IPath getSourceJarLocation(MavenCoordinates mavenCoordinates) {
    return new Path("/path/to/source/jar/file/in/m2_repo/" + mavenCoordinates.getArtifactId() + "." + mavenCoordinates.getType());
  }

  @Override
  public IPath getJavadocJarLocation(MavenCoordinates mavenCoordinates) {
    return new Path("/path/to/javadoc/jar/file/in/m2_repo/" + mavenCoordinates.getArtifactId() +
                    "." + mavenCoordinates.getType());
  }

  @Activate
  protected void activate() {
    mavenHelper = new M2EclipseMavenHelper();
  }

  @VisibleForTesting
  protected interface MavenHelper {
    Artifact resolveArtifact(IProgressMonitor monitor, MavenCoordinates coordinates,
                             List<ArtifactRepository> repositories) throws CoreException;

    ArtifactRepository createArtifactRepository(String host, String string) throws CoreException;
  }

  /*
   * To make sure that mavenHelper is not null in production, ensure that the activate() method is called.
   */
  @VisibleForTesting
  void setMavenHelper(MavenHelper mavenHelper) {
    this.mavenHelper = mavenHelper;
  }

  private static class M2EclipseMavenHelper implements MavenHelper {

    @Override
    public Artifact resolveArtifact(IProgressMonitor monitor,
                                    MavenCoordinates mavenCoordinates,
                                    List<ArtifactRepository> repositories) throws CoreException {
      return MavenUtils.resolveArtifact(null, mavenCoordinates.getGroupId(), mavenCoordinates.getArtifactId(),
                                        mavenCoordinates.getType(), mavenCoordinates.getVersion(),
                                        mavenCoordinates.getClassifier(), repositories);
    }

    @Override
    public ArtifactRepository createArtifactRepository(String id, String url) throws CoreException {
      return MavenUtils.createRepository(id, url);
    }
  }
}
