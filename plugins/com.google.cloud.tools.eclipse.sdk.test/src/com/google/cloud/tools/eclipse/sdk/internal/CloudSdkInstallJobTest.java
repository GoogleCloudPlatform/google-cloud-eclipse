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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVersionMismatchException;
import com.google.cloud.tools.managedcloudsdk.MessageListener;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExecutionException;
import com.google.cloud.tools.managedcloudsdk.command.CommandExitException;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponent;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponentInstaller;
import com.google.cloud.tools.managedcloudsdk.install.SdkInstaller;
import com.google.cloud.tools.managedcloudsdk.install.SdkInstallerException;
import com.google.cloud.tools.managedcloudsdk.install.UnknownArchiveTypeException;
import java.io.IOException;
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
public class CloudSdkInstallJobTest {

  @Mock private MessageConsoleStream consoleStream;
  @Mock private ManagedCloudSdk managedCloudSdk;
  @Mock private SdkInstaller sdkInstaller;
  @Mock private SdkComponentInstaller componentInstaller;

  @Before
  public void setUp() throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException,
      UnsupportedOsException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    when(managedCloudSdk.hasComponent(any(SdkComponent.class))).thenReturn(true);
    when(managedCloudSdk.newInstaller()).thenReturn(sdkInstaller);
    when(managedCloudSdk.newComponentInstaller()).thenReturn(componentInstaller);
  }

  @Test
  public void testBelongsTo() {
    Job installJob = new CloudSdkInstallJob(null, new ReentrantReadWriteLock());
    assertTrue(installJob.belongsTo(CloudSdkInstallJob.CLOUD_SDK_MODIFY_JOB_FAMILY));
  }

  @Test
  public void testMutexRuleSet() {
    Job installJob = new CloudSdkInstallJob(null, new ReentrantReadWriteLock());
    assertEquals(CloudSdkInstallJob.MUTEX_RULE, installJob.getRule());
  }

  @Test
  public void testGetManagedCloudSdk() throws UnsupportedOsException {
    assertNotNull(new CloudSdkInstallJob(null, new ReentrantReadWriteLock()).getManagedCloudSdk());
  }

  @Test
  public void testRun_consoleStreamOutput()
      throws InterruptedException, ManagedSdkVerificationException,
          ManagedSdkVersionMismatchException {
    when(managedCloudSdk.isInstalled()).thenReturn(false);
    when(managedCloudSdk.hasComponent(any(SdkComponent.class))).thenReturn(false);

    CloudSdkInstallJob job = newCloudSdkInstallJob();
    job.schedule();
    job.join();

    verify(consoleStream)
        .println("Installing/upgrading the Cloud SDK... (may take several minutes)");
  }

  @Test
  public void testRun_notInstalled() throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException,
      InterruptedException, UnsupportedOsException, IOException, SdkInstallerException, UnknownArchiveTypeException,
      CommandExecutionException, CommandExitException {
    when(managedCloudSdk.isInstalled()).thenReturn(false);
    when(managedCloudSdk.hasComponent(any(SdkComponent.class))).thenReturn(false);

    CloudSdkInstallJob job = newCloudSdkInstallJob();
    job.schedule();
    job.join();

    assertTrue(job.getResult().isOK());
    verify(managedCloudSdk).newInstaller();
    verify(managedCloudSdk).newComponentInstaller();
    verify(sdkInstaller).install(any(MessageListener.class));
    verify(componentInstaller).installComponent(any(SdkComponent.class), any(MessageListener.class));
  }

  @Test
  public void testFailureSeverity()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException,
          InterruptedException, UnsupportedOsException {
    when(managedCloudSdk.isInstalled()).thenReturn(false);
    when(managedCloudSdk.hasComponent(any(SdkComponent.class))).thenReturn(false);
    UnsupportedOsException osException = new UnsupportedOsException("unsupported");
    when(managedCloudSdk.newInstaller()).thenThrow(osException);

    CloudSdkInstallJob job = newCloudSdkInstallJob();
    job.setFailureSeverity(IStatus.WARNING);
    job.schedule();
    job.join();

    IStatus result = job.getResult();
    assertEquals(IStatus.WARNING, result.getSeverity());
    assertEquals(
        "Google Cloud SDK installation only supported on Windows, Linux, and MacOS.",
        result.getMessage());
    assertEquals(osException, result.getException());
  }

  @Test
  public void testRun_sdkInstalled_componentNotInstalled()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException,
          InterruptedException, UnsupportedOsException, CommandExecutionException,
          CommandExitException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    when(managedCloudSdk.hasComponent(any(SdkComponent.class))).thenReturn(false);

    CloudSdkInstallJob job = newCloudSdkInstallJob();
    job.schedule();
    job.join();

    assertTrue(job.getResult().isOK());
    verify(managedCloudSdk, never()).newInstaller();
    verify(managedCloudSdk).newComponentInstaller();
    verify(componentInstaller).installComponent(any(SdkComponent.class), any(MessageListener.class));
  }

  @Test
  public void testRun_alreadyInstalled() throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException,
      InterruptedException, UnsupportedOsException {
    when(managedCloudSdk.isInstalled()).thenReturn(true);
    when(managedCloudSdk.hasComponent(any(SdkComponent.class))).thenReturn(true);

    CloudSdkInstallJob job = newCloudSdkInstallJob();
    job.schedule();
    job.join();

    assertTrue(job.getResult().isOK());
    verify(managedCloudSdk, never()).newInstaller();
    verify(managedCloudSdk, never()).newComponentInstaller();
  }

  @Test
  public void testRun() throws InterruptedException {
    Job job = newCloudSdkInstallJob();
    job.schedule();
    job.join();
    assertTrue(job.getResult().isOK());
  }

  private CloudSdkInstallJob newCloudSdkInstallJob() {
    return new CloudSdkInstallJob(consoleStream, new ReentrantReadWriteLock()) {
      @Override
      protected ManagedCloudSdk getManagedCloudSdk() throws UnsupportedOsException {
        return managedCloudSdk;
      }
    };
  }
}
