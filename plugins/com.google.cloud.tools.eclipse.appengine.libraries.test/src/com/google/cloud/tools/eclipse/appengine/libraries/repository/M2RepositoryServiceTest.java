package com.google.cloud.tools.eclipse.appengine.libraries.repository;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;
import org.eclipse.core.runtime.IPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.cloud.tools.eclipse.appengine.libraries.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.M2RepositoryService.RepositorySystemSessionFactory;

@RunWith(MockitoJUnitRunner.class)
public class M2RepositoryServiceTest {

  private static final String TEST_VERSION = "1.2.3";
  private static final String FAKE_PATH = "/fake/path";

  @Mock private RepositorySystem repositorySystem;
  @Mock private Version version;
  @Mock private RepositorySystemSessionFactory repositorySystemSessionFactory;
  @Mock private Artifact artifact;

  @Captor private ArgumentCaptor<VersionRangeRequest> versionRangeRequestCaptor =
      ArgumentCaptor.forClass(VersionRangeRequest.class);
  @Captor private ArgumentCaptor<ArtifactRequest> artifactRequestCaptor =
      ArgumentCaptor.forClass(ArtifactRequest.class);

  @Before
  public void setUp() throws Exception {
    File file = mock(File.class);
    when(file.getAbsolutePath()).thenReturn(FAKE_PATH);
    when(artifact.getFile()).thenReturn(file);

    when(version.toString()).thenReturn(TEST_VERSION);
  }

  @Test(expected = LibraryRepositoryServiceException.class)
  public void testGetJarLocation_missingRepositorySystem() throws LibraryRepositoryServiceException {
    M2RepositoryService service = new M2RepositoryService();
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    service.getJarLocation(mavenCoordinates);
  }

  @Test
  public void testGetJarLocation_latestVersionSuccessful() throws LibraryRepositoryServiceException,
                                                                  VersionRangeResolutionException,
                                                                  ArtifactResolutionException {
    M2RepositoryService service = new M2RepositoryService();
    service.setRepositorySystem(repositorySystem);
    service.setRepositorySystemSessionFactory(repositorySystemSessionFactory);
    when(repositorySystem.resolveVersionRange(any(RepositorySystemSession.class), versionRangeRequestCaptor.capture()))
    .thenReturn(getVersionRangeResult());

    when(repositorySystem.resolveArtifact(any(RepositorySystemSession.class), artifactRequestCaptor.capture()))
    .thenReturn(getArtifactResult());

    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    IPath jarLocation = service.getJarLocation(mavenCoordinates);
    assertThat(jarLocation.toOSString(), is(FAKE_PATH));
    assertThat(versionRangeRequestCaptor.getValue().getArtifact().getVersion(), is("[0,)"));
    assertThat(artifactRequestCaptor.getValue().getArtifact().getVersion(), is(TEST_VERSION));
  }

  @Test
  public void testGetJarLocation_explicitVersionSuccessful() throws LibraryRepositoryServiceException,
                                                                    VersionRangeResolutionException,
                                                                    ArtifactResolutionException {
    M2RepositoryService service = new M2RepositoryService();
    service.setRepositorySystem(repositorySystem);
    service.setRepositorySystemSessionFactory(repositorySystemSessionFactory);

    when(repositorySystem.resolveArtifact(any(RepositorySystemSession.class), any(ArtifactRequest.class)))
    .thenReturn(getArtifactResult());

    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setVersion(TEST_VERSION);
    IPath jarLocation = service.getJarLocation(mavenCoordinates);
    assertThat(jarLocation.toOSString(), is(FAKE_PATH));
    verify(repositorySystem, never()).resolveVersionRange(any(RepositorySystemSession.class),
                                                          any(VersionRangeRequest.class));
  }

  @Test(expected = LibraryRepositoryServiceException.class)
  public void testGetJarLocation_artifactUnresolved() throws LibraryRepositoryServiceException,
                                                             ArtifactResolutionException {
    M2RepositoryService service = new M2RepositoryService();
    service.setRepositorySystem(repositorySystem);
    service.setRepositorySystemSessionFactory(repositorySystemSessionFactory);

    when(repositorySystem.resolveArtifact(any(RepositorySystemSession.class), any(ArtifactRequest.class)))
    .thenReturn(getUnresolvedArtifactResult());

    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setVersion(TEST_VERSION);
    service.getJarLocation(mavenCoordinates);
  }

  @Test(expected = LibraryRepositoryServiceException.class)
  public void testGetJarLocation_artifactResolutionExceptionIsThrown() throws LibraryRepositoryServiceException,
                                                                              ArtifactResolutionException {
    M2RepositoryService service = new M2RepositoryService();
    service.setRepositorySystem(repositorySystem);
    service.setRepositorySystemSessionFactory(repositorySystemSessionFactory);

    when(repositorySystem.resolveArtifact(any(RepositorySystemSession.class), any(ArtifactRequest.class)))
    .thenThrow(new ArtifactResolutionException(Collections.<ArtifactResult>emptyList()));

    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setVersion(TEST_VERSION);
    service.getJarLocation(mavenCoordinates);
  }

  @Test(expected = LibraryRepositoryServiceException.class)
  public void testGetJarLocation_versionRangeResolutionExceptionIsThrown() throws LibraryRepositoryServiceException,
                                                                                  VersionRangeResolutionException {
    M2RepositoryService service = new M2RepositoryService();
    service.setRepositorySystem(repositorySystem);
    service.setRepositorySystemSessionFactory(repositorySystemSessionFactory);

    when(repositorySystem.resolveVersionRange(any(RepositorySystemSession.class), any(VersionRangeRequest.class)))
    .thenThrow(new VersionRangeResolutionException(null));

    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    service.getJarLocation(mavenCoordinates);
  }

  @Test(expected = LibraryRepositoryServiceException.class)
  public void testGetJarLocation_invalidRepositoryId() throws LibraryRepositoryServiceException,
                                                              VersionRangeResolutionException {
    M2RepositoryService service = new M2RepositoryService();
    service.setRepositorySystem(repositorySystem);
    service.setRepositorySystemSessionFactory(repositorySystemSessionFactory);

    when(repositorySystem.resolveVersionRange(any(RepositorySystemSession.class), any(VersionRangeRequest.class)))
    .thenThrow(new VersionRangeResolutionException(null));

    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setRepository("invalid_repository_id");
    service.getJarLocation(mavenCoordinates);
  }

  @Test(expected = LibraryRepositoryServiceException.class)
  public void testGetJarLocation_invalidRepositoryURI() throws LibraryRepositoryServiceException,
                                                               VersionRangeResolutionException {
    M2RepositoryService service = new M2RepositoryService();
    service.setRepositorySystem(repositorySystem);
    service.setRepositorySystemSessionFactory(repositorySystemSessionFactory);

    when(repositorySystem.resolveVersionRange(any(RepositorySystemSession.class), any(VersionRangeRequest.class)))
    .thenThrow(new VersionRangeResolutionException(null));

    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setRepository("http://");
    service.getJarLocation(mavenCoordinates);
  }

  @Test
  public void testGetJarLocation_customRepositoryURI() throws LibraryRepositoryServiceException,
                                                              VersionRangeResolutionException,
                                                              ArtifactResolutionException {
    M2RepositoryService service = new M2RepositoryService();
    service.setRepositorySystem(repositorySystem);
    service.setRepositorySystemSessionFactory(repositorySystemSessionFactory);

    when(repositorySystem.resolveVersionRange(any(RepositorySystemSession.class), versionRangeRequestCaptor.capture()))
    .thenReturn(getVersionRangeResult());

    when(repositorySystem.resolveArtifact(any(RepositorySystemSession.class), artifactRequestCaptor.capture()))
    .thenReturn(getArtifactResult());

    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setRepository("http://example.com");
    IPath jarLocation = service.getJarLocation(mavenCoordinates);

    assertThat(jarLocation.toOSString(), is(FAKE_PATH));
    assertThat(versionRangeRequestCaptor.getValue().getArtifact().getVersion(), is("[0,)"));
    assertThat(artifactRequestCaptor.getValue().getArtifact().getVersion(), is(TEST_VERSION));

  }

  private ArtifactResult getUnresolvedArtifactResult() {
    return new ArtifactResult(new ArtifactRequest());
  }

  private ArtifactResult getArtifactResult() {
    ArtifactResult artifactResult = new ArtifactResult(new ArtifactRequest());
    artifactResult.setArtifact(artifact);
    return artifactResult;
  }

  private VersionRangeResult getVersionRangeResult() {
    VersionRangeResult versionRangeResult = new VersionRangeResult(new VersionRangeRequest());
    versionRangeResult.setVersions(Collections.singletonList(version));
    return versionRangeResult;
  }
}
