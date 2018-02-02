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
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkInstallJob;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.After;
import org.junit.Test;

public class CloudSdkManagerTest {

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
  public void testRunInstallJob_blocking() throws CoreException, InterruptedException {
    CloudSdkInstallJob okJob = new FakeInstallJob(Status.OK_STATUS);
    CloudSdkManager.runInstallJob(null, okJob);
    // Incomplete test, but if it ever fails, something is surely broken.
    assertEquals(Job.NONE, okJob.getState());
  }

  @Test
  public void testRunInstallJob_canceled() throws InterruptedException {
    try {
      CloudSdkManager.runInstallJob(null, new FakeInstallJob(Status.CANCEL_STATUS));
      fail();
    } catch (CoreException e) {
      assertEquals(Status.CANCEL, e.getStatus().getSeverity());
    }
  }

  @Test
  public void testRunInstallJob_installError() throws InterruptedException {
    try {
      IStatus errorResult = StatusUtil.error(this, "awesome install error in unit test");
      CloudSdkManager.runInstallJob(null, new FakeInstallJob(errorResult));
      fail();
    } catch (CoreException e) {
      assertEquals(Status.ERROR, e.getStatus().getSeverity());
      assertEquals("awesome install error in unit test", e.getMessage());
    }
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
