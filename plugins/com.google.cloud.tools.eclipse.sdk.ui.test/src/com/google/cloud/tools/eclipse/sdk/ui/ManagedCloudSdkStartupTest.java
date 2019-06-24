/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.sdk.ui;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.cloud.tools.eclipse.sdk.CloudSdkManager;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVersionMismatchException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ManagedCloudSdkStartupTest {
  @Mock private CloudSdkManager sdkManager;
  @Mock private ManagedCloudSdk installation;
  @Mock private IWorkbench workbench;
  @Mock private Display display;

  @Before
  public void setUp() {
    doNothing().when(sdkManager).installManagedSdkAsync();
    doNothing().when(sdkManager).updateManagedSdkAsync();
    doReturn(display).when(workbench).getDisplay();
  }

  @Test
  public void testNotifiedIfNotInstalled()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException {
    doReturn(false).when(installation).isInstalled();
    doReturn(false).when(installation).isUpToDate();

    ManagedCloudSdkStartup startup = new ManagedCloudSdkStartup(workbench);
    startup.checkInstallation(sdkManager, installation, null);
    verify(installation).isInstalled();
    // indirectly check that the user was notified of the update
    verify(workbench).getDisplay();
    verify(display).asyncExec(any(Runnable.class));
    verifyNoMoreInteractions(sdkManager, installation, workbench, display);
  }

  @Test
  public void testNotifiedIfInstalledAndOutOfDate()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException {
    doReturn(true).when(installation).isInstalled();
    doReturn(false).when(installation).isUpToDate();

    ManagedCloudSdkStartup startup = new ManagedCloudSdkStartup(workbench);
    startup.checkInstallation(sdkManager, installation, null);
    verify(installation).isInstalled();
    verify(installation).isUpToDate();
    verify(installation).getSdkHome(); // used to get version
    // indirectly check that the user was notified of the update
    verify(workbench).getDisplay();
    verify(display).asyncExec(any(Runnable.class));
    verifyNoMoreInteractions(sdkManager, installation, workbench, display);
  }

  @Test
  public void testInstalledAndUpToDate()
      throws ManagedSdkVerificationException, ManagedSdkVersionMismatchException {
    doReturn(true).when(installation).isInstalled();
    doReturn(true).when(installation).isUpToDate();

    ManagedCloudSdkStartup startup = new ManagedCloudSdkStartup(workbench);
    startup.checkInstallation(sdkManager, installation, null);
    verify(installation).isInstalled();
    verify(installation).isUpToDate();
    verifyNoMoreInteractions(sdkManager, installation, workbench, display);
  }
}
