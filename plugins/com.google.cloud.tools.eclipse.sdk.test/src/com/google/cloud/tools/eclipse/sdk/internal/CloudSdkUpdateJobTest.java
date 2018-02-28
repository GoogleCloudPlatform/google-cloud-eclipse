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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.managedcloudsdk.ConsoleListener;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVersionMismatchException;
import com.google.cloud.tools.managedcloudsdk.ProgressListener;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExecutionException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExitException;
import com.google.cloud.tools.managedcloudsdk.update.SdkUpdater;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsoleStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudSdkUpdateJobTest {

  @Mock private MessageConsoleStream consoleStream;
  @Mock private ManagedCloudSdk managedCloudSdk;
  @Mock private SdkUpdater sdkUpdater;

  @Before
  public void setUp() throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    when(managedCloudSdk.newUpdater()).thenReturn(sdkUpdater);
  }

  @Test
  public void testBelongsTo() {
    Job updateJob = newCloudSdkUpdateJob();
    assertTrue(updateJob.belongsTo(CloudSdkUpdateJob.CLOUD_SDK_MODIFY_JOB_FAMILY));
  }

  @Test
  public void testMutexRuleSet() {
    Job updateJob = newCloudSdkUpdateJob();
    assertEquals(CloudSdkUpdateJob.MUTEX_RULE, updateJob.getRule());
  }

  @Test
  public void testGetManagedCloudSdk() throws UnsupportedOsException {
    assertNotNull(newCloudSdkUpdateJob().getManagedCloudSdk());
  }

  @Test
  public void testRun_errorWhenNotInstalled()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException,
          InterruptedException {
    when(managedCloudSdk.isInstalled()).thenReturn(false);

    CloudSdkUpdateJob job = newCloudSdkUpdateJob();
    job.schedule();
    job.join();

    IStatus result = job.getResult();
    assertEquals(IStatus.ERROR, job.getResult().getSeverity());
    assertEquals("Google Cloud SDK is not installed", result.getMessage());
  }

  @Test
  public void testFailureSeverity()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException,
          InterruptedException {
    when(managedCloudSdk.isInstalled()).thenReturn(false);

    CloudSdkUpdateJob job = newCloudSdkUpdateJob();
    job.setFailureSeverity(IStatus.WARNING);
    job.schedule();
    job.join();

    IStatus result = job.getResult();
    assertEquals(IStatus.WARNING, result.getSeverity());
    assertEquals("Google Cloud SDK is not installed", result.getMessage());
  }

  @Test
  public void testRun_sdkInstalled_noUpdateIfUpToDate()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException,
          InterruptedException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    when(managedCloudSdk.isUpToDate()).thenReturn(true);

    CloudSdkUpdateJob job = newCloudSdkUpdateJob();
    job.schedule();
    job.join();

    assertTrue(job.getResult().isOK());
    verify(managedCloudSdk).isInstalled();
    verify(managedCloudSdk).isUpToDate();
    verify(managedCloudSdk).getSdkHome(); // used to log version
    verify(managedCloudSdk, never()).newUpdater();
    verifyNoMoreInteractions(managedCloudSdk);
  }

  @Test
  public void testRun_sdkInstalled_updateSuccess()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException,
          InterruptedException, CommandExecutionException, CommandExitException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    when(managedCloudSdk.isUpToDate()).thenReturn(false);
    when(managedCloudSdk.newUpdater()).thenReturn(sdkUpdater);

    CloudSdkUpdateJob job = newCloudSdkUpdateJob();
    job.schedule();
    job.join();

    assertTrue(job.getResult().isOK());
    verify(managedCloudSdk).isInstalled();
    verify(managedCloudSdk).isUpToDate();
    verify(managedCloudSdk).newUpdater();
    verify(sdkUpdater).update(any(ProgressListener.class), any(ConsoleListener.class));
    verify(managedCloudSdk, times(2)).getSdkHome(); // used to log old and new versions
    verifyNoMoreInteractions(managedCloudSdk);
  }

  @Test
  public void testRun_sdkInstalled_updateFailed()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException,
          InterruptedException, CommandExitException, CommandExecutionException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    when(managedCloudSdk.isUpToDate()).thenReturn(false);
    when(managedCloudSdk.newUpdater()).thenReturn(sdkUpdater);
    CommandExecutionException exception = new CommandExecutionException(new RuntimeException());
    doThrow(exception).when(sdkUpdater)
        .update(any(ProgressListener.class), any(ConsoleListener.class));

    CloudSdkUpdateJob job = newCloudSdkUpdateJob();
    job.schedule();
    job.join();

    assertEquals(IStatus.ERROR, job.getResult().getSeverity());
    assertEquals(exception, job.getResult().getException());
    verify(managedCloudSdk).isInstalled();
    verify(managedCloudSdk).isUpToDate();
    verify(managedCloudSdk).newUpdater();
    verify(sdkUpdater).update(any(ProgressListener.class), any(ConsoleListener.class));
    verify(managedCloudSdk).getSdkHome(); // used to obtain old version
    verifyNoMoreInteractions(managedCloudSdk);
  }

  private CloudSdkUpdateJob newCloudSdkUpdateJob() {
    return new CloudSdkUpdateJob(consoleStream, new ReentrantReadWriteLock()) {
      @Override
      protected ManagedCloudSdk getManagedCloudSdk() throws UnsupportedOsException {
        return managedCloudSdk;
      }
    };
  }
}
