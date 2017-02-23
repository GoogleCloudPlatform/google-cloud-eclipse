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

package com.google.cloud.tools.eclipse.util.io;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.http.TestHttpServer;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests that use an HTTP server. Separate from {@link FileDownloaderTest} to avoid unnecessary test server launching
 * in cases when not required.
 */
public class FileDownloaderWithServerTest {

  private static final String FILE_TO_DOWNLOAD = "index.html";
  private static final String FILE_CONTENT = "<html><body>hello</body></html>";

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule public TestHttpServer server = new TestHttpServer(FILE_TO_DOWNLOAD, FILE_CONTENT);

  @Test
  public void testDownload_successful() throws IOException {
    FileDownloader fileDownloader = new FileDownloader(new Path(temporaryFolder.newFolder().getAbsolutePath()));
    IPath downloadPath = fileDownloader.download(new URL(server.getAddress() + FILE_TO_DOWNLOAD), new NullProgressMonitor());
    assertNotNull(downloadPath);
    File downloadedFile = downloadPath.toFile();
    assertTrue(downloadedFile.exists());
    assertThat(downloadedFile.getName(), is(FILE_TO_DOWNLOAD));
    assertThat(new String(Files.readAllBytes(downloadedFile.toPath()), StandardCharsets.UTF_8),
               is(FILE_CONTENT));
  }
}
