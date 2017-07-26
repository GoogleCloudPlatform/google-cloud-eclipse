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

package com.google.cloud.tools.eclipse.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CloudSdkProcessFacadeTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testForDeploy_nullCredentialFile() {
    try {
      CloudSdkProcessFacade.forDeploy(null, null);
      fail();
    } catch (NullPointerException ex) {
      assertEquals(ex.getMessage(), "credential required for deploying");
    }
  }

  @Test
  public void testForDeploy_nonExistingCredentialFile() {
    try {
      Path credential = tempFolder.getRoot().toPath().resolve("non-existing-file");
      assertFalse(Files.exists(credential));
      CloudSdkProcessFacade.forDeploy(credential, null);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals(ex.getMessage(), "non-existing credential file");
    }
  }

  @Test
  public void testGetCloudSdk_forDeploy() throws IOException {
    Path credential = tempFolder.newFile().toPath();
    CloudSdkProcessFacade facade = CloudSdkProcessFacade.forDeploy(credential, null);
    assertNotNull(facade.getCloudSdk());
  }

}
