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

import com.google.cloud.tools.eclipse.appengine.libraries.persistence.LibraryClasspathContainerSerializer;
import com.google.cloud.tools.eclipse.test.util.http.TestHttpServer;
import java.net.URL;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RemoteFileSourceAttachmentDownloaderJobTest extends AbstractSourceAttachmentDownloaderJobTest {

  private static final String SOURCE_ARTIFACT_NAME = "sourceArtifact.zip";

  @Rule
  public TestHttpServer fileServer = new TestHttpServer(temporaryFolder, SOURCE_ARTIFACT_NAME,
                                                        "Source artifact contents");

  private URL sourceUrl;
  private Path downloadFolder; 
  
  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    sourceUrl = new URL(fileServer.getAddress() + "/" + SOURCE_ARTIFACT_NAME);
    downloadFolder = new Path(temporaryFolder.newFolder().getAbsolutePath());
  }
  
  @Override
  protected String getSourceArtifactFilePath() {
    return downloadFolder.append(SOURCE_ARTIFACT_NAME).toFile().getAbsolutePath();
  }

  @Override
  protected Job createJob(IJavaProject javaProject, 
                          LibraryClasspathContainerSerializer serializer) {
    return new RemoteFileSourceAttachmentDownloaderJob(javaProject, new Path(LIBRARY_PATH),
                                                       serializer, downloadFolder, sourceUrl);
  }
}
