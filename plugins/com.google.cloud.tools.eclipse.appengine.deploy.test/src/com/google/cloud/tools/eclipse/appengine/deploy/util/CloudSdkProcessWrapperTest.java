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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.appengine.operations.AppEngineWebXmlProjectStaging;
import com.google.cloud.tools.appengine.operations.Deployment;
import com.google.cloud.tools.appengine.operations.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider.State;
import org.eclipse.core.runtime.Status;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CloudSdkProcessWrapperTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private final CloudSdkProcessWrapper wrapper = new CloudSdkProcessWrapper();

  @Before
  public void setUp() {
    TestAccountProvider.setAsDefaultProvider(State.NOT_LOGGED_IN);
  }
  
  @Test
  public void testGetAppEngineDeployment_nullCredentialFile() throws CloudSdkNotFoundException {
    try {
      wrapper.getAppEngineDeployment(null);
      fail();
    } catch (IllegalStateException ex) {
      assertEquals(ex.getMessage(), "credential required for deploying");
    }
  }

  @Test
  public void testGetAppEngineDeployment() throws CloudSdkNotFoundException {
    TestAccountProvider.setProviderState(State.LOGGED_IN);
    Deployment deployment = wrapper.getAppEngineDeployment(null);
    assertNotNull(deployment);
  }

  @Test
  public void testGetAppEngineStandardStaging() throws CloudSdkNotFoundException {
    AppEngineWebXmlProjectStaging staging = wrapper.getAppEngineStandardStaging(null, null, null);
    assertNotNull(staging);
  }

  @Test
  public void testGetAppEngineDeployment_cannotSetUpTwice()
      throws CloudSdkNotFoundException {
    TestAccountProvider.setProviderState(State.LOGGED_IN);
    wrapper.getAppEngineDeployment(null);
    try {
      wrapper.getAppEngineDeployment(null);
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
