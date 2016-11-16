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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.M2RepositoryService.MavenHelper;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.io.File;
import java.net.URI;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class M2RepositoryServiceTest {

  private static final String FAKE_PATH = "/fake/path";

  @Mock private MavenHelper mavenHelper;
  @Mock private MavenCoordinatesClasspathAttributesTransformer transformer;

  private M2RepositoryService m2RepositoryService;
  
  @Before
  public void setUp() {
    m2RepositoryService = new M2RepositoryService();
    m2RepositoryService.setMavenHelper(mavenHelper);
    m2RepositoryService.setTransformer(transformer);
  }
  
  @Test(expected = LibraryRepositoryServiceException.class)
  public void getLibraryClasspathEntry_errorInArtifactResolution() throws Exception {
    when(mavenHelper.resolveArtifact(any(IProgressMonitor.class), any(MavenCoordinates.class),
                                     anyListOf(ArtifactRepository.class)))
      .thenThrow(testCoreException());

    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    m2RepositoryService.getLibraryClasspathEntry(new LibraryFile(mavenCoordinates));
  }

  @Test(expected = LibraryRepositoryServiceException.class)
  public void getLibraryClasspathEntry_invalidRepositoryId() throws LibraryRepositoryServiceException {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setRepository("invalid_repository_id");
    m2RepositoryService.getLibraryClasspathEntry(new LibraryFile(mavenCoordinates));
  }

  @Test(expected = LibraryRepositoryServiceException.class)
  public void getLibraryClasspathEntry_invalidRepositoryURI() throws LibraryRepositoryServiceException {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setRepository("http://");
    m2RepositoryService.getLibraryClasspathEntry(new LibraryFile(mavenCoordinates));
  }

  @Test(expected = LibraryRepositoryServiceException.class)
  public void getLibraryClasspathEntry_customRepositoryCreationError() throws Exception {
    when(mavenHelper.createArtifactRepository("example.com", "http://example.com")).thenThrow(testCoreException());

    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setRepository("http://example.com");
    m2RepositoryService.getLibraryClasspathEntry(new LibraryFile(mavenCoordinates));
  }

  @Test
  public void getLibraryClasspathEntry_customRepositoryURI() throws Exception {
    when(mavenHelper.createArtifactRepository("example.com", "http://example.com"))
      .thenReturn(mock(ArtifactRepository.class));
    Artifact artifact = getMockArtifactWithJarPath();
    when(mavenHelper.resolveArtifact(any(IProgressMonitor.class),
                                     any(MavenCoordinates.class),
                                     anyListOf(ArtifactRepository.class))).thenReturn(artifact);
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    mavenCoordinates.setRepository("http://example.com");
    IPath jarLocation = m2RepositoryService.getLibraryClasspathEntry(new LibraryFile(mavenCoordinates)).getPath();

    assertThat(jarLocation.toOSString(), is(FAKE_PATH));
  }

  @Test
  public void getLibraryClasspathEntry_withJavadoc() throws Exception {
    Artifact artifact = getMockArtifactWithJarPath();
    when(mavenHelper.resolveArtifact(any(IProgressMonitor.class),
                                     any(MavenCoordinates.class),
                                     anyListOf(ArtifactRepository.class))).thenReturn(artifact);
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setJavadocUri(new URI("http://example.com/javadoc"));
    IClasspathAttribute[] attributes = m2RepositoryService.getLibraryClasspathEntry(libraryFile).getExtraAttributes();

    assertThat(attributes, hasItemInArray(allOf(hasProperty("name",
                                                            is(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME)),
                                                hasProperty("value",
                                                            is("http://example.com/javadoc")))));
  }

  @Test
  public void getLibraryClasspathEntry_withInvalidSourceUriMavenSourceResolutionHappens() throws Exception {
    Artifact artifact = getMockArtifactWithJarPath();
    when(mavenHelper.resolveArtifact(any(IProgressMonitor.class),
                                     any(MavenCoordinates.class),
                                     anyListOf(ArtifactRepository.class))).thenReturn(artifact);
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setSourceUri(new URI("foo/bar"));
    IPath sourcePath = m2RepositoryService.getLibraryClasspathEntry(libraryFile).getSourceAttachmentPath();
    assertEquals(new Path(FAKE_PATH), sourcePath);
  }

  @Test(expected = IllegalStateException.class)
  public void testMavenHelperMustBeSet() throws LibraryRepositoryServiceException {
    new M2RepositoryService().getLibraryClasspathEntry(new LibraryFile(new MavenCoordinates("groupId", "artifactId")));
  }

  @Test(expected = TestRuntimeException.class)
  public void testActivateSetsMavenHelper() throws LibraryRepositoryServiceException {
    M2RepositoryService m2RepositoryService = new M2RepositoryService();
    m2RepositoryService.activate();
    MavenCoordinates mavenCoordinates = mock(MavenCoordinates.class);
    // TestRuntimeException is thrown to verify that it was thrown because of the mock setup
    when(mavenCoordinates.getRepository()).thenThrow(new TestRuntimeException());
    m2RepositoryService.getLibraryClasspathEntry(new LibraryFile(mavenCoordinates));
  }

  private Artifact getMockArtifactWithJarPath() {
    Artifact artifact = mock(Artifact.class);
    File file = new File(FAKE_PATH);
    when(artifact.getFile()).thenReturn(file );
    return artifact;
  }

  private CoreException testCoreException() {
    return new CoreException(StatusUtil.error(this, "Test exception"));
  }

  @SuppressWarnings("serial")
  private static class TestRuntimeException extends RuntimeException {
  }
}
