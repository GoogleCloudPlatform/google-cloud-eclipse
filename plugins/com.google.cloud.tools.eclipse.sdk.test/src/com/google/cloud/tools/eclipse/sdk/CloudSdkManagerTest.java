/*
 * Copyright 2018 Google Inc.
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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkInstallJob;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVersionMismatchException;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudSdkManagerTest {

  @Mock private ManagedCloudSdk managedCloudSdk;
  @Mock private IProgressMonitor monitor;

  @Before
  public void setUp() throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    when(managedCloudSdk.hasComponent(any(SdkComponent.class))).thenReturn(true);
  }

  @After
  public void tearDown() {
    CloudSdkManager.forceManagedSdkFeature = false;
  }

  @Test
  public void testManagedSdkOption() {
    assertFalse(CloudSdkManager.isManagedSdkFeatureEnabled());
  }

  @Test
  public void testManagedSdkOption_featureForced() {
    CloudSdkManager.forceManagedSdkFeature = true;
    assertTrue(CloudSdkManager.isManagedSdkFeatureEnabled());
  }

  @Test
  public void testRunInstallJob_blocking() {
    CloudSdkInstallJob okJob = new FakeInstallJob(Status.OK_STATUS);
    IStatus result = CloudSdkManager.runInstallJob(null, okJob, monitor);
    // Incomplete test, but if it ever fails, something is surely broken.
    assertEquals(Job.NONE, okJob.getState());
    assertTrue(result.isOK());
  }

  @Test
  public void testRunInstallJob_canceled() {
    CloudSdkInstallJob cancelJob = new FakeInstallJob(Status.CANCEL_STATUS);
    IStatus result = CloudSdkManager.runInstallJob(null, cancelJob, monitor);
    assertEquals(Job.NONE, cancelJob.getState());
    assertEquals(Status.CANCEL, result.getSeverity());
  }

  @Test
  public void testRunInstallJob_installError() {
    IStatus error = StatusUtil.error(this, "awesome install error in unit test");
    CloudSdkInstallJob errorJob = new FakeInstallJob(error);
    IStatus result = CloudSdkManager.runInstallJob(null, errorJob, monitor);
    assertEquals(Job.NONE, errorJob.getState());
    assertEquals(Status.ERROR, result.getSeverity());
    assertEquals("awesome install error in unit test", result.getMessage());
  }

  private static class FakeInstallJob extends CloudSdkInstallJob {

    private final IStatus result;

    private FakeInstallJob(IStatus result) {
      super(null);
      this.result = result;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      return result;
    }
  }
}
