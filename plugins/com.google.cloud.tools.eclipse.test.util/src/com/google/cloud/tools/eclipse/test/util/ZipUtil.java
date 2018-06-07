/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.test.util;

import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Utilities for {@code .zip} files.
 */
public class ZipUtil {
  /** Unzip the contents into the specified destination directory. */
  public static IStatus unzip(File zip, File destination, IProgressMonitor monitor) {
    SubMonitor progress = SubMonitor.convert(monitor);
    if (!destination.exists()) {
      if (!destination.mkdirs()) {
        return StatusUtil.error(ZipUtil.class, "Unable to create destination: " + destination);
      }
    } else if (!destination.isDirectory()) {
      return StatusUtil.error(ZipUtil.class, "Destination is not a directory: " + destination);
    }

    try (ZipFile zipFile = new ZipFile(zip)) {
      String canonicalDestination = destination.getCanonicalPath();

      progress.setWorkRemaining(zipFile.size());
      for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements()
          && !progress.isCanceled();) {
        ZipEntry entry = entries.nextElement();
        File entryLocation = new File(destination, entry.getName());
        if (!entryLocation.getCanonicalPath().startsWith(canonicalDestination + File.separator)) {
          return StatusUtil.error(
              ZipUtil.class, "Blocked unzipping file outside the destination: " + entry.getName());
        }

        if (entry.isDirectory()) {
          if (!entryLocation.exists()) {
            if (!entryLocation.mkdirs()) {
              return StatusUtil.error(ZipUtil.class,
                  "Unable to create destination: " + entryLocation);
            }
          } else if (!entryLocation.isDirectory()) {
            return StatusUtil.error(ZipUtil.class,
                "Destination is not a directory: " + entryLocation);
          }
        } else {
          try (InputStream input = zipFile.getInputStream(entry);
              FileOutputStream output = new FileOutputStream(entryLocation)) {
            ByteStreams.copy(input, output);
          }
        }
        progress.worked(1);
      }
      return Status.OK_STATUS;
    } catch (IOException ex) {
      return StatusUtil.error(ZipUtil.class, "Unable to extract zip file: " + zip, ex);
    }
  }

  public static File extractZip(URL zipProjectLocation, File destination) throws IOException {
    URL zipLocation = FileLocator.toFileURL(zipProjectLocation);
    if (!zipLocation.getProtocol().equals("file")) {
      throw new IOException("could not resolve location to a file");
    }
    File zippedFile = new File(zipLocation.getPath());
    assertTrue(zippedFile.exists());
    IStatus status = unzip(zippedFile, destination, new NullProgressMonitor());
    assertTrue("failed to extract: " + status, status.isOK());
    return destination;
  }

}
