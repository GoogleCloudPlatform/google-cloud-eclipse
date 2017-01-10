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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import java.io.File;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class M2SourceAttachmentDownloaderJobTest extends AbstractSourceAttachmentDownloaderJobTest {

  @Mock
  private MavenHelper mavenHelper;
  @Mock
  private Artifact binaryArtifact;
  private File sourceArtifactFile;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    sourceArtifactFile = temporaryFolder.newFile("testSourceArtifact");
    when(mavenHelper.getMavenSourceJarLocation(any(Artifact.class), any(IProgressMonitor.class)))
      .thenReturn(new Path(sourceArtifactFile.getAbsolutePath()));
    when(binaryArtifact.getGroupId()).thenReturn("groupId");
    when(binaryArtifact.getArtifactId()).thenReturn("artifactId");
  }

  @Override
  protected String getSourceArtifactFilePath() {
    return sourceArtifactFile.getAbsolutePath();
  }

  @Override
  protected Job createJob(IJavaProject javaProject,
                          LibraryClasspathContainerSerializer serializer) {
    return new M2SourceAttachmentDownloaderJob(javaProject, new Path(LIBRARY_PATH), serializer,
                                               binaryArtifact, mavenHelper);
  }
}
