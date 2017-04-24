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

package com.google.cloud.tools.eclipse.sdk.internal;

import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.util.io.DeleteAllVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class to generate a mock Google Cloud SDK installation that passes
 * {@link com.google.cloud.tools.appengine.cloudsdk.CloudSdk#validateCloudSdk()}.
 */
public class MockSdkGenerator {
  /**
   * Create a mock Google Cloud SDK installation. It is the caller's responsibility to remove this
   * directory with {@link #deleteMockSdk(Path)}.
   */
  public static Path createMockSdk() throws Exception {
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
    return mockSdk;
  }

  /** Delete a created mock SDK. */
  public static void deleteMockSdk(Path mockSdk) throws IOException {
    Files.walkFileTree(mockSdk, new DeleteAllVisitor());
  }

  private static void createEmptyFile(Path path) throws Exception {
    Files.createDirectories(path.getParent());
    assertTrue(path.getParent().toFile().isDirectory());
    Files.createFile(path);
    assertTrue(Files.isRegularFile(path));
  }

  private MockSdkGenerator() {
  }

}
