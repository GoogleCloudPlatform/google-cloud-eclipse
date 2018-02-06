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

package com.google.cloud.tools.eclipse.sdk.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.sdk.MockedSdkInstallJob;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVersionMismatchException;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsoleStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudSdkInstallJobTest {

  @Mock private MessageConsoleStream consoleStream;
  @Mock private ManagedCloudSdk managedCloudSdk;

  @Before
  public void setUp() throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    when(managedCloudSdk.hasComponent(any(SdkComponent.class))).thenReturn(true);
  }

  @Test
  public void testBelongsTo() {
    Job installJob = new CloudSdkInstallJob(null);
    assertTrue(installJob.belongsTo(CloudSdkInstallJob.CLOUD_SDK_MODIFY_JOB_FAMILY));
  }

  @Test
  public void testMutexRuleSet() {
    Job installJob = new CloudSdkInstallJob(null);
    assertEquals(CloudSdkInstallJob.MUTEX_RULE, installJob.getRule());
  }

  @Test
  public void testRun_mutualExclusion() throws InterruptedException {
    MockedSdkInstallJob job1 = scheduleBlockingJobAndWaitUntilRunning();
    MockedSdkInstallJob job2 = new MockedSdkInstallJob(true /* blockBeforeExit */, managedCloudSdk);

    job2.schedule();
    // Incomplete test, but if it ever fails, something is surely broken.
    assertNotEquals(Job.RUNNING, job2.getState());

    job1.unblock();
    job2.unblock();
    job1.join();
    job2.join();
  }

  private MockedSdkInstallJob scheduleBlockingJobAndWaitUntilRunning() throws InterruptedException {
    MockedSdkInstallJob job = new MockedSdkInstallJob(true /* blockBeforeExit */, managedCloudSdk);

    job.schedule();
    while (job.getState() != Job.RUNNING) {
      Thread.sleep(10);
    }
    return job;
  }
}
