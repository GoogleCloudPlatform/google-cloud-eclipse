/*
 * Copyright 2017 Google Inc.
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.test.util.http.TestHttpServer;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.net.URI;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Separate class to test downloading source from remote location and to avoid creating the test
 * file server for each tests in {@link M2RepositoryServiceTest} unnecessarily. 
 */
@RunWith(MockitoJUnitRunner.class)
public class M2RepositoryServiceRemoteFileDownloadTest {

  private static final String FAKE_PATH = "/fake/path";
  private static final String SOURCE_FILE_NAME = "sourceArtifact.zip";
  private static final String SOURCE_FILE_CONTENT = "Contents of the source artifact";

  private M2RepositoryService m2RepositoryService;
  
  @Mock private MavenHelper mavenHelper;
  @Mock private MavenCoordinatesHelper transformer;
  @Mock private SourceDownloaderJobFactory sourceDownloaderJobFactory;
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule public TestHttpServer httpServer = new TestHttpServer(temporaryFolder,
                                                              SOURCE_FILE_NAME,
                                                              SOURCE_FILE_CONTENT);
  @Before
  public void setUp() throws Exception {
    m2RepositoryService = new M2RepositoryService();
    m2RepositoryService.setMavenHelper(mavenHelper);
    m2RepositoryService.setTransformer(transformer);
    m2RepositoryService.setSourceDownloaderJobFactory(sourceDownloaderJobFactory);
  }

  @Test
  public void getLibraryClasspathEntry_withoutJavaProjectNoBackgroundJobIsExecuted() throws Exception {
    Artifact artifact = mock(Artifact.class);
    File file = new File(FAKE_PATH);
    when(artifact.getFile()).thenReturn(file );
    when(artifact.getGroupId()).thenReturn("groupId");
    when(artifact.getArtifactId()).thenReturn("artifactId");
    when(artifact.getVersion()).thenReturn("1.0.0");
    when(mavenHelper.resolveArtifact(any(MavenCoordinates.class),
                                     any(IProgressMonitor.class))).thenReturn(artifact);

    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setSourceUri(new URI(httpServer.getAddress() + "/" + SOURCE_FILE_NAME));

    IClasspathEntry classpathEntry =
        m2RepositoryService.getLibraryClasspathEntry(null /* javaProject */,
                                                     libraryFile,
                                                     new NullProgressMonitor());
    File downloadedSourceFile = classpathEntry.getSourceAttachmentPath().toFile();
    assertTrue(downloadedSourceFile.exists());
    assertThat(Files.toString(downloadedSourceFile, Charsets.UTF_8), is(SOURCE_FILE_CONTENT));
  }

}
