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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.deploy.util.CloudSdkProcessWrapper.ProcessExitRecorder;
import com.google.cloud.tools.eclipse.sdk.CollectingLineListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.eclipse.core.runtime.Status;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CloudSdkProcessWrapperTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private final CloudSdkProcessWrapper wrapper = new CloudSdkProcessWrapper();

  @Test
  public void testSetUpDeployCloudSdk_nullCredentialFile() {
    try {
      wrapper.setUpDeployCloudSdk(null, null);
      fail();
    } catch (NullPointerException ex) {
      assertEquals(ex.getMessage(), "credential required for deploying");
    }
  }

  @Test
  public void testSetUpDeployCloudSdk_nonExistingCredentialFile() {
    Path credential = tempFolder.getRoot().toPath().resolve("non-existing-file");
    assertFalse(Files.exists(credential));

    try {
      wrapper.setUpDeployCloudSdk(credential, null);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals(ex.getMessage(), "non-existing credential file");
    }
  }

  @Test
  public void testGetCloudSdk_beforeSetUp() {
    try {
      wrapper.getCloudSdk();
      fail();
    } catch (NullPointerException ex) {
      assertEquals("wrapper not set up", ex.getMessage());
    }
  }

  @Test
  public void testGetCloudSdk_forDeploy() throws IOException {
    Path credential = tempFolder.newFile().toPath();
    wrapper.setUpDeployCloudSdk(credential, null);
    assertNotNull(wrapper.getCloudSdk());
  }

  @Test
  public void testGetCloudSdk_forStandardStaging() {
    wrapper.setUpStandardStagingCloudSdk(null, null, null);
    assertNotNull(wrapper.getCloudSdk());
  }

  @Test
  public void testSetUpDeployCloudSdk_cannotSetUpTwice() throws IOException {
    Path credential = tempFolder.newFile().toPath();
    wrapper.setUpDeployCloudSdk(credential, null);
    try {
      wrapper.setUpDeployCloudSdk(credential, null);
      fail();
    } catch (IllegalStateException ex) {
      assertEquals(ex.getMessage(), "CloudSdk already set up");
    }
  }

  @Test
  public void testSetUpStandardStagingCloudSdk_cannotSetUpTwice() {
    wrapper.setUpStandardStagingCloudSdk(null, null, null);
    try {
      wrapper.setUpStandardStagingCloudSdk(null, null, null);
      fail();
    } catch (IllegalStateException ex) {
      assertEquals(ex.getMessage(), "CloudSdk already set up");
    }
  }

  @Test
  public void testProcessExitRecorder_onErrorExitWithNullErrorMessageCollector() {
    wrapper.setUpStandardStagingCloudSdk(null, null, null);

    ProcessExitRecorder recorder = wrapper.new ProcessExitRecorder(null /* errorMessageCollector */);
    recorder.onExit(15);

    assertEquals(Status.ERROR, wrapper.getExitStatus().getSeverity());
    assertEquals("Process exited with error code 15", wrapper.getExitStatus().getMessage());
  }

  @Test
  public void testProcessExitRecorder_onErrorExit() {
    wrapper.setUpStandardStagingCloudSdk(null, null, null);
    CollectingLineListener errorMessageCollector = mock(CollectingLineListener.class);
    when(errorMessageCollector.getCollectedMessages()).thenReturn(Arrays.asList("got ", " errors"));

    ProcessExitRecorder recorder = wrapper.new ProcessExitRecorder(errorMessageCollector);
    recorder.onExit(23);

    assertEquals(Status.ERROR, wrapper.getExitStatus().getSeverity());
    assertEquals("got \n errors", wrapper.getExitStatus().getMessage());
  }

  @Test
  public void testProcessExitRecorder_onOkExit() {
    wrapper.setUpStandardStagingCloudSdk(null, null, null);

    ProcessExitRecorder recorder = wrapper.new ProcessExitRecorder(null /* errorMessageCollector */);
    recorder.onExit(0);

    assertTrue(wrapper.getExitStatus().isOK());
  }
}
