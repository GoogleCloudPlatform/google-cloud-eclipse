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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.cli.transfer.QuietMavenTransferListener;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.eclipse.aether.version.Version;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.MavenPlugin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.google.cloud.tools.eclipse.appengine.libraries.MavenCoordinates;
import com.google.common.annotations.VisibleForTesting;

import io.takari.aether.connector.AetherRepositoryConnectorFactory;

/**
 * Implementation of {@link ILibraryRepositoryService} that relies on M2Eclipse to download the artifacts and store
 * them in the local Maven repository pointed to by M2Eclipse's M2_REPO variable.
 */
@Component
public class M2RepositoryService implements ILibraryRepositoryService {

  private static final String AETHER_ALL_VERSION_RANGE = "[0,)";

  private static final Logger logger = Logger.getLogger(M2RepositoryService.class.getName());

  private static final RemoteRepository MAVEN_CENTRAL =
      new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build();

  private RepositorySystem repositorySystem;

  private RepositorySystemSessionFactory repositorySystemSessionFactory;

  @Override
  public IPath getJarLocation(MavenCoordinates mavenCoordinates) throws LibraryRepositoryServiceException {
    try {
      if (repositorySystem == null) {
        // in tests setRepositorySystem() can be used instead of activate()
        throw new LibraryRepositoryServiceException("repositorySystem was not initialized, ensure that activate() is before any operations");
      }

      RepositorySystemSession session = newRepositorySystemSession(repositorySystem);

      List<RemoteRepository> repository = getRepository(mavenCoordinates);

      String version = getVersion(mavenCoordinates);

      Artifact artifact = getArtifact(mavenCoordinates, version);

      if (mavenCoordinates.getVersion().equals(MavenCoordinates.LATEST_VERSION)) {
        Version latestVersion = findLatestVersion(repositorySystem, session, artifact, repository);
        artifact = artifact.setVersion(latestVersion.toString());
      }

      ArtifactResult artifactResult = resolveArtifact(repositorySystem, session, repository, artifact);
      if (artifactResult.isMissing()) {
        throw new LibraryRepositoryServiceException("Could not resolve maven artifact: " + mavenCoordinates);
      }

      return new Path(artifactResult.getArtifact().getFile().getAbsolutePath());
    } catch (ArtifactResolutionException | VersionRangeResolutionException ex) {
      throw new LibraryRepositoryServiceException("Could not resolve maven artifact: " + mavenCoordinates, ex);
    }
  }

  private RepositorySystem newRepositorySystem() {
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, AetherRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, WagonTransporterFactory.class);
    locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
      @Override
      public void serviceCreationFailed(Class<?> serviceClass, Class<?> implementationClass, Throwable exception) {
        logger.log(Level.SEVERE,
                   "Failed to initialize service of class "
                       + serviceClass.getName()
                       + " with implementation class of "
                       + implementationClass.getName(),
                       exception);
      }
    });

    return locator.getService(RepositorySystem.class);
  }

  private RepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
    return repositorySystemSessionFactory.getSession(system);
  }

  private RemoteRepository getRepository(String repository) throws LibraryRepositoryServiceException {
    try {
      URI repoUri = new URI(repository);
      if (!repoUri.isAbsolute()) {
        throw new LibraryRepositoryServiceException("repository URI must be an absolute URI (i.e. has to have a "
            + "schema component)");
      }
      return new RemoteRepository.Builder(repoUri.getHost(), "default", repoUri.toString()).build();
    } catch (URISyntaxException exception) {
      throw new LibraryRepositoryServiceException("repository is not a valid URI and currenlty only 'central' is "
          + "supported as repository ID",
          exception);
    }
  }

  private String getVersion(MavenCoordinates mavenCoordinates) {
    String version;
    if (mavenCoordinates.getVersion().equals(MavenCoordinates.LATEST_VERSION)) {
      version = AETHER_ALL_VERSION_RANGE;
    } else {
      version = mavenCoordinates.getVersion();
    }
    return version;
  }

  private Artifact getArtifact(MavenCoordinates mavenCoordinates, String version) {
    return new DefaultArtifact(mavenCoordinates.getGroupId(),
                               mavenCoordinates.getArtifactId(),
                               mavenCoordinates.getClassifier(),
                               mavenCoordinates.getType(),
                               version);
  }

  private Version findLatestVersion(RepositorySystem system,
                                    RepositorySystemSession session,
                                    Artifact artifact,
                                    List<RemoteRepository> mavenCentral) throws VersionRangeResolutionException {
    VersionRangeRequest rangeRequest = new VersionRangeRequest();
    rangeRequest.setArtifact(artifact);
    rangeRequest.setRepositories(mavenCentral);
    VersionRangeResult versionRangeResult = system.resolveVersionRange(session, rangeRequest);
    Version latestVersion = versionRangeResult.getHighestVersion();
    return latestVersion;
  }

  private List<RemoteRepository> getRepository(MavenCoordinates mavenCoordinates) throws LibraryRepositoryServiceException {
    List<RemoteRepository> repository;
    if (mavenCoordinates.getRepository().equals(MavenCoordinates.MAVEN_CENTRAL_REPO)) {
      repository = Collections.singletonList(MAVEN_CENTRAL);
    } else {
      repository = Collections.singletonList(getRepository(mavenCoordinates.getRepository()));
    }
    return repository;
  }

  private ArtifactResult resolveArtifact(RepositorySystem repositorySystem,
                                         RepositorySystemSession session,
                                         List<RemoteRepository> repository,
                                         Artifact artifact) throws ArtifactResolutionException {
    ArtifactRequest artifactRequest = new ArtifactRequest();
    artifactRequest.setArtifact(artifact);
    artifactRequest.setRepositories(repository);
    ArtifactResult artifactResult = repositorySystem.resolveArtifact(session, artifactRequest);
    return artifactResult;
  }

  @Override
  public IPath getSourceJarLocation(MavenCoordinates mavenCoordinates) {
    return new Path("/path/to/source/jar/file/in/m2_repo/" + mavenCoordinates.getArtifactId() + "." + mavenCoordinates.getType());
  }

  @Activate
  protected void activate() throws LibraryRepositoryServiceException {
    repositorySystem = newRepositorySystem();
    if (repositorySystem == null) {
      throw new LibraryRepositoryServiceException("Could not create instance of " + RepositorySystem.class.getName());
    }
    repositorySystemSessionFactory = new DefaultRepositorySystemSessionFactory();
  }

  @VisibleForTesting
  void setRepositorySystem(RepositorySystem repositorySystem) {
    this.repositorySystem = repositorySystem;
  }

  @VisibleForTesting
  void setRepositorySystemSessionFactory(RepositorySystemSessionFactory repositorySystemSessionFactory) {
    this.repositorySystemSessionFactory = repositorySystemSessionFactory;
  }

  @VisibleForTesting
  interface RepositorySystemSessionFactory {
    RepositorySystemSession getSession(RepositorySystem system);
  }

  private static class DefaultRepositorySystemSessionFactory implements RepositorySystemSessionFactory {
    @Override
    public RepositorySystemSession getSession(RepositorySystem system) {
      DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

      LocalRepository localRepo =
          new LocalRepository(MavenPlugin.getRepositoryRegistry().getLocalRepository().getBasedir());
      session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

      session.setTransferListener(new QuietMavenTransferListener());
      session.setRepositoryListener(new AbstractRepositoryListener() { });

      return session;
    }

  }
}
