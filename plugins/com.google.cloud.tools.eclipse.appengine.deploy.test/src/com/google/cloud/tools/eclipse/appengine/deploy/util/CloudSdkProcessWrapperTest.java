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

package com.google.cloud.tools.eclipse.appengine.deploy.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.appengine.api.deploy.AppEngineDeployment;
import com.google.cloud.tools.appengine.api.deploy.AppEngineStandardStaging;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.runtime.Status;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CloudSdkProcessWrapperTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private final CloudSdkProcessWrapper wrapper = new CloudSdkProcessWrapper();

  @Test
  public void testGetAppEngineDeployment_nullCredentialFile() throws CloudSdkNotFoundException {
    try {
      wrapper.getAppEngineDeployment(null, null);
      fail();
    } catch (NullPointerException ex) {
      assertEquals(ex.getMessage(), "credential required for deploying");
    }
  }

  @Test
  public void testGetAppEngineDeployment_nonExistingCredentialFile()
      throws CloudSdkNotFoundException {
    Path credential = tempFolder.getRoot().toPath().resolve("non-existing-file");
    assertFalse(Files.exists(credential));

    try {
      wrapper.getAppEngineDeployment(credential, null);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals(ex.getMessage(), "non-existing credential file");
    }
  }

  @Test
  public void testGetAppEngineDeployment() throws IOException, CloudSdkNotFoundException {
    Path credential = tempFolder.newFile().toPath();
    AppEngineDeployment deployment = wrapper.getAppEngineDeployment(credential, null);
    assertNotNull(deployment);
  }

  @Test
  public void testGetAppEngineStandardStaging() throws CloudSdkNotFoundException {
    AppEngineStandardStaging staging = wrapper.getAppEngineStandardStaging(null, null, null);
    assertNotNull(staging);
  }

  @Test
  public void testGetAppEngineDeployment_cannotSetUpTwice()
      throws IOException, CloudSdkNotFoundException {
    Path credential = tempFolder.newFile().toPath();
    wrapper.getAppEngineDeployment(credential, null);
    try {
      wrapper.getAppEngineDeployment(credential, null);
      fail();
    } catch (IllegalStateException ex) {
      assertEquals(ex.getMessage(), "process wrapper already set up");
    }
  }

  @Test
  public void testGetAppEngineStandardStaging_cannotSetUpTwice() throws CloudSdkNotFoundException {
    wrapper.getAppEngineStandardStaging(null, null, null);
    try {
      wrapper.getAppEngineStandardStaging(null, null, null);
      fail();
    } catch (IllegalStateException ex) {
      assertEquals(ex.getMessage(), "process wrapper already set up");
    }
  }

  @Test
  public void testProcessExitRecorder_onErrorExitWithNullErrorMessageCollector() {
    wrapper.recordProcessExitCode(15);

    assertEquals(Status.ERROR, wrapper.getExitStatus().getSeverity());
    assertEquals("Process exited with error code 15", wrapper.getExitStatus().getMessage());
  }

  @Test
  public void testProcessExitRecorder_onErrorExit() {
    wrapper.recordProcessExitCode(235);

    assertEquals(Status.ERROR, wrapper.getExitStatus().getSeverity());
  }

  @Test
  public void testProcessExitRecorder_onOkExit() {
    wrapper.recordProcessExitCode(0);

    assertTrue(wrapper.getExitStatus().isOK());
  }
}
