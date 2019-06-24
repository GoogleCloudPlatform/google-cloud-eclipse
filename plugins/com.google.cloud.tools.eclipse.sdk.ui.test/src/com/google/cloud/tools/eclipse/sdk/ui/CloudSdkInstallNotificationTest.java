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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudSdkInstallNotificationTest {
  @Mock private IWorkbench workbench;
  @Mock private Runnable trigger;

  @Before
  public void setUp() {
    when(workbench.getDisplay()).thenReturn(Display.getCurrent());
  }

  @Test
  public void testDefaults() {
    CloudSdkInstallNotification notification = new CloudSdkInstallNotification(workbench, trigger);
    assertTrue(notification.shouldInstall);
  }

  @Test
  public void testTriggerOnCloseButton() {
    CloudSdkInstallNotification notification = new CloudSdkInstallNotification(workbench, trigger);
    notification.open();
    assertTrue(notification.shouldInstall);
    notification.close();
    verify(trigger).run();
  }

  @Test
  public void testTriggeredOnFade() {
    CloudSdkInstallNotification notification = new CloudSdkInstallNotification(workbench, trigger);
    // simulate fade away
    notification.open();
    notification.getShell().setAlpha(0);
    assertTrue(notification.shouldInstall);
    notification.close();
    verify(trigger).run();
  }

  @Test
  public void testNotTriggeredOnSkipLink() {
    CloudSdkInstallNotification notification = new CloudSdkInstallNotification(workbench, trigger);
    notification.linkSelected("skip");
    assertFalse(notification.shouldInstall);
    notification.close();
    verify(trigger, never()).run();
  }
}
