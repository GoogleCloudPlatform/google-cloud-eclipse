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

package com.google.cloud.tools.eclipse.test.util;

import static org.junit.Assert.assertTrue;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class to generate a mock Google Cloud SDK installation that passes
 * {@code com.google.cloud.tools.appengine.operations.CloudSdk#validateCloudSdk()}.
 */
public class MockSdkGenerator {
  /**
   * Create a mock Google Cloud SDK installation. It is the caller's responsibility to remove this
   * directory with {@link #deleteMockSdk(Path)}.
   */
  public static Path createMockSdk() throws Exception {
    return createMockSdk("184.0.0");
  }

  public static Path createMockSdk(String sdkVersion) throws IOException {
    Path mockSdk = Files.createTempDirectory("mock-gcloud-sdk");
    assertTrue(mockSdk.toFile().isDirectory());

    createEmptyFile(mockSdk.resolve("bin/gcloud"));
    createEmptyFile(mockSdk.resolve("bin/gcloud.cmd"));
    createEmptyFile(mockSdk.resolve("bin/dev_appserver.py"));
    createEmptyFile(mockSdk.resolve(
        "platform/google_appengine/google/appengine/tools/java/lib/appengine-tools-api.jar"));
    createEmptyFile(mockSdk.resolve(
        "platform/google_appengine/google/appengine/tools/java/lib/shared/servlet-api.jar"));
    createEmptyFile(mockSdk
        .resolve("platform/google_appengine/google/appengine/tools/java/lib/shared/jsp-api.jar"));
    createEmptyFile(mockSdk.resolve("platform/bundledpython/python.exe"));
    createFile(mockSdk.resolve("VERSION"), sdkVersion);
    return mockSdk;
  }

  /** Delete a created mock SDK. */
  public static void deleteMockSdk(Path mockSdk) throws IOException {
    MoreFiles.deleteRecursively(mockSdk, RecursiveDeleteOption.ALLOW_INSECURE);
  }

  private static void createEmptyFile(Path path) throws IOException {
    createFile(path, "");
  }

  private static void createFile(Path path, String content) throws IOException {
    Files.createDirectories(path.getParent());
    assertTrue(path.getParent().toFile().isDirectory());
    Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    assertTrue(Files.isRegularFile(path));
  }

  private MockSdkGenerator() {
  }

}
