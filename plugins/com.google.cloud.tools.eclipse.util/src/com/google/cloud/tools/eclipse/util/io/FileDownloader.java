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

import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.common.base.Preconditions;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

/**
 * Utility class to download files from {@link URL}s.
 */
public class FileDownloader {

  private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;
  private static final int DEFAULT_READ_TIMEOUT_MS = 3000;

  private final IPath downloadFolderPath;

  /**
   * Creates a new instance which will download the files to the directory defined by
   * <code>downloadFolderPath</code>.
   * <p>
   * If the directory does not exist, it will be created on-demand when the first file is
   * downloaded.
   *
   * @param downloadFolderPath path to the directory where the files must be downloaded. Cannot be
   *        <code>null</code>, but does not have to exist. It will be created on demand.
   */
  public FileDownloader(IPath downloadFolderPath) {
    Preconditions.checkNotNull(downloadFolderPath, "downloadFolderPath is null");
    File downloadFolder = downloadFolderPath.toFile();
    Preconditions.checkArgument(!downloadFolder.exists() || downloadFolder.isDirectory());
    this.downloadFolderPath = downloadFolderPath;
  }

  /**
   * Downloads the file pointed to by the <code>url</code>
   * <p>
   * The downloaded file's name will be the last segment of the path of the URL.
   *
   * @param url location of the file to download, cannot be <code>null</code>
   * @return a path pointing to the downloaded file
   * @throws IOException if the URL cannot be opened, the output file cannot be written or the
   *         transfer of the remote file fails
   */
  public IPath download(URL url, IProgressMonitor monitor) throws IOException {
    Preconditions.checkNotNull(url, "url is null");
    String lastSegment = new Path(url.getPath()).lastSegment();
    Preconditions.checkNotNull(lastSegment, "last segment is null");
    Preconditions.checkArgument(!lastSegment.isEmpty(), "last segment is empty string");

    File downloadedFile = downloadFolderPath.append(lastSegment).toFile();
    if (downloadedFile.exists()) {
      return new Path(downloadedFile.getAbsolutePath());
    }

    ensureDownloadFolderExists();
    URLConnection connection = url.openConnection();
    connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MS);
    connection.setReadTimeout(DEFAULT_READ_TIMEOUT_MS);
    connection.setRequestProperty("User-Agent", CloudToolsInfo.USER_AGENT);
    try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
         OutputStream outputStream =
             new BufferedOutputStream(new FileOutputStream(downloadedFile))) {
      byte[] buffer = new byte[4096];
      int read = 0;
      while ((read = inputStream.read(buffer)) != -1) {
        if (monitor.isCanceled()) {
          return null;
        }
        outputStream.write(buffer, 0, read);
      }
      return new Path(downloadedFile.getAbsolutePath());
    } finally {
      if (monitor.isCanceled()) {
        Files.delete(downloadedFile.toPath());
      }
    }
  }

  private void ensureDownloadFolderExists() throws IOException {
    File downloadFolder = downloadFolderPath.toFile();
    if (!downloadFolder.exists() && !downloadFolder.mkdirs()) {
      throw new IOException("Cannot create folder " + downloadFolder.getAbsolutePath());
    }
  }
}
