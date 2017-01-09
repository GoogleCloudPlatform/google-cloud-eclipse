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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SourceDownloaderJobFactoryTest {

  private static final String REMOTE_URL = "http://example.org/foo.zip";

  @Mock private MavenHelper mavenHelper;
  @Mock private IJavaProject javaProject;
  @Mock private Artifact artifact;
  @Mock private IPath classpathEntryPath;

  @Test
  public void testCreateM2BasedJobWithoutSourceUrl() {
    Job job =
        new SourceDownloaderJobFactory(mavenHelper).createSourceDownloaderJob(javaProject,
                                                                              artifact,
                                                                              classpathEntryPath,
                                                                              null /* sourceURL */);
    assertThat(job, instanceOf(M2SourceAttachmentDownloaderJob.class));
  }

  @Test
  public void testCreateRemoteFileDownloaderJobWithSourceUrl() throws MalformedURLException {
    when(artifact.getGroupId()).thenReturn("groupId");
    when(artifact.getArtifactId()).thenReturn("artifactId");
    when(artifact.getVersion()).thenReturn("1.0.0");

    Job job =
        new SourceDownloaderJobFactory(mavenHelper).createSourceDownloaderJob(javaProject,
                                                                              artifact,
                                                                              classpathEntryPath,
                                                                              new URL(REMOTE_URL));
    assertThat(job, instanceOf(RemoteFileSourceAttachmentDownloaderJob.class));
  }

}
