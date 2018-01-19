/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.sdk.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.sdk.CloudSdkManager;
import com.google.cloud.tools.eclipse.sdk.internal.PreferenceConstants;
import com.google.cloud.tools.eclipse.sdk.ui.preferences.CloudSdkPreferenceArea;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.io.File;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for CloudSdkPreferenceArea.
 */
@RunWith(MockitoJUnitRunner.class)
public class CloudSdkPreferenceAreaTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock
  private IPreferenceStore preferences;

  private CloudSdkPreferenceArea area;
  private Shell shell;

  private Button managedSdkRadio;
  private Button autoUpdateCheck;
  private Button updateNow;
  private Button customSdkRadio;
  private Text sdkLocation;

  @Test
  public void testNonExistentPath() {
    when(preferences.getString(PreferenceConstants.CLOUD_SDK_PATH)).thenReturn("/non-existent");
    createPreferenceArea();

    assertFalse(area.getStatus().isOK());
    assertEquals(IStatus.ERROR, area.getStatus().getSeverity());
  }

  @Test
  public void testInvalidPath() {
    when(preferences.getString(PreferenceConstants.CLOUD_SDK_PATH))
        .thenReturn(tempFolder.getRoot().getAbsolutePath());
    createPreferenceArea();
    assertEquals(IStatus.WARNING, area.getStatus().getSeverity());

    when(preferences.getString(PreferenceConstants.CLOUD_SDK_PATH)).thenReturn("");
    area.load();
    assertEquals(IStatus.OK, area.getStatus().getSeverity());
  }

  // TODO(chanseok): can become "@Before setUp()" once we remove the managed SDK debug feature flag.
  private void createPreferenceArea() {
    shell = shellResource.getShell();
    area = new CloudSdkPreferenceArea();
    area.setPreferenceStore(preferences);
    area.createContents(shell);
    area.load();

    managedSdkRadio = CompositeUtil.findButton(shell, "Managed SDK");
    autoUpdateCheck = CompositeUtil.findButton(shell, "Update automatically");
    updateNow = CompositeUtil.findButton(shell, "Update Now");
    customSdkRadio = CompositeUtil.findButton(shell, "Custom SDK");
    sdkLocation = CompositeUtil.findControlAfterLabel(shell, Text.class, "&SDK location:");
  }

  @Test
  public void testUi_noSdkManagementFeature() {
    when(preferences.getString(PreferenceConstants.CLOUD_SDK_PATH)).thenReturn("");
    createPreferenceArea();

    assertNull(managedSdkRadio);
    assertNull(autoUpdateCheck);
    assertNull(updateNow);
    assertNull(customSdkRadio);
    assertNotNull(sdkLocation);
    assertTrue(sdkLocation.isEnabled());
  }

  @Test
  public void testUi_sdkManagementFeature() {
    CloudSdkManager.forceManagedSdkFeature = true;
    when(preferences.getString(PreferenceConstants.CLOUD_SDK_PATH)).thenReturn("");
    createPreferenceArea();

    assertNotNull(managedSdkRadio);
    assertNotNull(autoUpdateCheck);
    assertNotNull(updateNow);
    assertNotNull(customSdkRadio);
    assertNotNull(sdkLocation);
  }

  @Test
  public void testControlStates_managedSdk() {
    CloudSdkManager.forceManagedSdkFeature = true;
    when(preferences.getString(PreferenceConstants.CLOUD_SDK_MANAGEMENT)).thenReturn("MANAGED");
    when(preferences.getBoolean(PreferenceConstants.CLOUD_SDK_AUTO_UPDATE)).thenReturn(true);
    when(preferences.getString(PreferenceConstants.CLOUD_SDK_PATH)).thenReturn("");
    createPreferenceArea();

    assertTrue(managedSdkRadio.getSelection());
    assertTrue(autoUpdateCheck.isEnabled());
    assertTrue(autoUpdateCheck.getSelection());
    assertTrue(updateNow.isEnabled());
    assertFalse(customSdkRadio.getSelection());
    assertFalse(sdkLocation.isEnabled());
  }

  @Test
  public void testControlStates_customSdk() {
    CloudSdkManager.forceManagedSdkFeature = true;
    when(preferences.getString(PreferenceConstants.CLOUD_SDK_MANAGEMENT)).thenReturn("CUSTOM");
    when(preferences.getBoolean(PreferenceConstants.CLOUD_SDK_AUTO_UPDATE)).thenReturn(true);
    when(preferences.getString(PreferenceConstants.CLOUD_SDK_PATH)).thenReturn("");
    createPreferenceArea();

    assertFalse(managedSdkRadio.getSelection());
    assertFalse(autoUpdateCheck.isEnabled());
    assertTrue(autoUpdateCheck.getSelection());
    assertFalse(updateNow.isEnabled());
    assertTrue(customSdkRadio.getSelection());
    assertTrue(sdkLocation.isEnabled());
  }
}
