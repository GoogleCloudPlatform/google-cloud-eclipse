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

package com.google.cloud.tools.eclipse.sdk.ui.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.sdk.CloudSdkManager;
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkPreferences;
import com.google.cloud.tools.eclipse.test.util.MockSdkGenerator;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for CloudSdkPreferenceArea.
 */
@RunWith(MockitoJUnitRunner.class)
public class CloudSdkPreferenceAreaTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock private CloudSdkManager cloudSdkManager;

  // delegate to get default-defaults
  private IPreferenceStore preferences =
      mock(IPreferenceStore.class, AdditionalAnswers.delegatesTo(new PreferenceStore()));

  private CloudSdkPreferenceArea area;
  private Shell shell;

  private Button chooseSdk;
  private Text sdkLocation;
  private Label sdkVersion;
  private Button updateSdkButton;

  @Test
  public void testNonExistentPath() {
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT)).thenReturn("MANUAL");
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_PATH)).thenReturn("/non-existent");
    createPreferenceArea();

    assertFalse(area.getStatus().isOK());
    assertEquals(IStatus.ERROR, area.getStatus().getSeverity());
    assertEquals("SDK version: No SDK found", sdkVersion.getText());
    // in manual mode we always have a location, so tooltip should be set
    assertEquals("/non-existent", sdkVersion.getToolTipText());
  }

  @Test
  public void testVersion() throws IOException {
    Path mockSdk = MockSdkGenerator.createMockSdk("1.23.4");
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_PATH)).thenReturn(mockSdk.toString());
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT)).thenReturn("MANUAL");

    createPreferenceArea();
    assertEquals("SDK version: 1.23.4", sdkVersion.getText());
    assertEquals(mockSdk.toString(), sdkVersion.getToolTipText());

    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT)).thenReturn("AUTOMATIC");
    area.load();
    assertNotEquals("SDK version: 1.23.4", sdkVersion.getText());
    // should be a different location
    assertNotEquals(mockSdk.toString(), sdkVersion.getToolTipText());
  }

  @Test
  public void testInvalidPath() {
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT)).thenReturn("MANUAL");
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_PATH))
        .thenReturn(tempFolder.getRoot().getAbsolutePath());
    createPreferenceArea();
    assertEquals(IStatus.WARNING, area.getStatus().getSeverity());

    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_PATH)).thenReturn("");
    area.load();
    assertTrue(area.getStatus().isOK());
  }

  // TODO(chanseok): can become "@Before setUp()" once we remove the managed SDK debug feature flag.
  private void createPreferenceArea() {
    shell = shellResource.getShell();
    area = new CloudSdkPreferenceArea(cloudSdkManager);
    area.setPreferenceStore(preferences);
    area.createContents(shell);
    area.load();

    chooseSdk = CompositeUtil.findButton(shell, "Choose SDK");
    sdkLocation = CompositeUtil.findControlAfterLabel(shell, Text.class, "&SDK location:");
    sdkVersion = CompositeUtil.findLabel(shell, "SDK version:");
    updateSdkButton = CompositeUtil.findButton(shell, "Update");
  }

  @Test
  public void testUi_sdkManagementFeature() {
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_PATH)).thenReturn("");
    createPreferenceArea();

    assertNotNull(chooseSdk);
    assertNotNull(sdkLocation);
    assertNotNull(sdkVersion);
  }
  
  // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2897
  @Test
  public void testSearchSdkIfSdkLocationIsEmpty() {
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_PATH)).thenReturn("");
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT)).thenReturn("MANUAL");
    
    createPreferenceArea();
    
    assertTrue(sdkLocation.getText().endsWith("google-cloud-sdk"));
    assertTrue(sdkVersion.getText().length() > 3);
    assertEquals(sdkLocation.getText(), sdkVersion.getToolTipText());
  }

  @Test
  public void testControlStates_automaticSdk() {
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT)).thenReturn("AUTOMATIC");
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_PATH)).thenReturn("");
    createPreferenceArea();

    assertFalse(chooseSdk.getSelection());
    assertFalse(sdkLocation.isEnabled());
  }

  @Test
  public void testControlStates_manualSdk() {
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT)).thenReturn("MANUAL");
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_PATH)).thenReturn("");
    createPreferenceArea();

    assertTrue(chooseSdk.getSelection());
    assertTrue(sdkLocation.isEnabled());
  }

  @Test
  public void testPerformApply_preferencesSaved() {
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT)).thenReturn("AUTOMATIC");
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_PATH)).thenReturn("");
    createPreferenceArea();

    assertFalse(chooseSdk.getSelection());
    chooseSdk.setSelection(true);
    area.performApply();

    verify(preferences).putValue(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT, "MANUAL");
  }

  @Test
  public void testValidationStatus_switchManagementOption() {
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT)).thenReturn("MANUAL");
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_PATH))
        .thenReturn("/non-existing/directory");
    createPreferenceArea();

    assertTrue(chooseSdk.getSelection());
    assertEquals(IStatus.ERROR, area.getStatus().getSeverity());

    new SWTBotCheckBox(chooseSdk).click();
    assertTrue(area.getStatus().isOK());

    new SWTBotCheckBox(chooseSdk).click();
    assertEquals(IStatus.ERROR, area.getStatus().getSeverity());
  }

  @Test
  public void testApply_automatic() {
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT)).thenReturn("MANUAL");
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_PATH))
        .thenReturn("/non-existing/directory");

    createPreferenceArea();
    new SWTBotCheckBox(chooseSdk).click();
    assertTrue(area.getStatus().isOK());

    area.performApply();
    verify(cloudSdkManager).installManagedSdkAsync();
    verifyNoMoreInteractions(cloudSdkManager);
  }

  @Test
  public void testUpdateSdk_manualSdkDisabled() {
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT)).thenReturn("MANUAL");
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_PATH)).thenReturn("/non-existent");

    createPreferenceArea();
    assertNotNull(updateSdkButton);
    assertTrue(updateSdkButton.getVisible()); // isVisible checks parent
    assertFalse(updateSdkButton.isEnabled());
  }

  @Test
  public void testUpdateSdk_autoSdkEnabled() {
    when(preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT)).thenReturn("AUTOMATIC");

    createPreferenceArea();
    assertTrue(updateSdkButton.getVisible()); // isVisible checks parent
    assertTrue(updateSdkButton.isEnabled());
  }
}
