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

import java.io.IOException;
import java.net.URL;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileDownloaderTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test(expected = NullPointerException.class)
  public void testConstructor_null() {
    new FileDownloader(null);
  }

  @Test
  public void testConstructor_emptyPathAllowed() {
    new FileDownloader(new Path(""));
  }

  @Test(expected = NullPointerException.class)
  public void testDownload_null() throws IOException {
    FileDownloader fileDownloader = new FileDownloader(new Path(temporaryFolder.newFolder().getAbsolutePath()));
    fileDownloader.download(null, new NullProgressMonitor());
  }

  @Test(expected = IOException.class)
  public void testDownload_cannotCreateDownloadFolder() throws IOException {
    FileDownloader fileDownloader = new FileDownloader(new Path("/dev/null/foo"));
    fileDownloader.download(new URL("http://example.com/dummy.zip"), new NullProgressMonitor());
  }
}
