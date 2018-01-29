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
import static org.junit.Assert.assertFalse;

import com.google.cloud.tools.eclipse.sdk.CloudSdkManager;
import com.google.cloud.tools.eclipse.test.util.TestPreferencesRule;
import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class CloudSdkPreferencesTest {
  @Rule public TestPreferencesRule preferencesCreator = new TestPreferencesRule();
  private boolean forceManagedSdkFeature;

  @Before
  public void setUp() {
    forceManagedSdkFeature = CloudSdkManager.forceManagedSdkFeature;
    CloudSdkManager.forceManagedSdkFeature = true;
  }

  @After
  public void tearDown() {
    CloudSdkManager.forceManagedSdkFeature = forceManagedSdkFeature;
  }

  @Test
  public void testInitializeDefaults() {
    IPreferenceStore preferences = preferencesCreator.getPreferenceStore();
    new CloudSdkPreferences().initializeDefaultPreferences(preferences);

    assertEquals(
        "AUTOMATIC", preferences.getDefaultString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
  }

  @Test
  public void testInitializeDefaults_noChangeIfSet() {
    IPreferenceStore preferences = preferencesCreator.getPreferenceStore();
    preferences.setValue(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT, "FOO");
    new CloudSdkPreferences().initializeDefaultPreferences(preferences);

    assertEquals("FOO", preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
    assertEquals(
        "AUTOMATIC", preferences.getDefaultString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
  }

  @Test
  public void testConfigure_noCloudSdkAvailable() {
    IPreferenceStore preferences = preferencesCreator.getPreferenceStore();

    CloudSdkPreferences.configureManagementPreferences(preferences, false /*cloudSdkAvailable*/);
    assertFalse(preferences.isDefault(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
    assertEquals("AUTOMATIC", preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
  }

  @Test
  public void testConfigure_cloudSdkAvailable() {
    IPreferenceStore preferences = preferencesCreator.getPreferenceStore();

    CloudSdkPreferences.configureManagementPreferences(preferences, true /*cloudSdkAvailable*/);
    assertFalse(preferences.isDefault(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
    assertEquals("MANUAL", preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
  }

  @Test
  public void testConfigure_hasCloudSdkPath() {
    IPreferenceStore preferences = preferencesCreator.getPreferenceStore();
    preferences.putValue(CloudSdkPreferences.CLOUD_SDK_PATH, "/a/path");

    CloudSdkPreferences.configureManagementPreferences(preferences, false /*cloudSdkAvailable*/);
    assertFalse(preferences.isDefault(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
    assertEquals("MANUAL", preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
  }
}
