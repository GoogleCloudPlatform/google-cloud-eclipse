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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.cloud.tools.eclipse.test.util.TestPreferencesRule;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

public class CloudSdkPreferencesTest {
  @Rule public TestPreferencesRule preferencesCreator = new TestPreferencesRule();

  private IPreferenceStore preferences;

  @Before
  public void setUp() {
    preferences = preferencesCreator.getPreferenceStore();
  }

  @Test
  public void testInitializeDefaults() {
    CloudSdkPreferences.initializeDefaultPreferences(preferences);

    assertEquals(
        "AUTOMATIC", preferences.getDefaultString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
  }

  @Test
  public void testIsAutoManaging_uninitialized() {
    assertFalse(CloudSdkPreferences.isAutoManaging(preferences));
  }

  @Test
  public void testIsAutoManaging_automatic() {
    preferences.setValue(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT, "AUTOMATIC");
    assertTrue(CloudSdkPreferences.isAutoManaging(preferences));
  }

  @Test
  public void testIsAutoManaging_noAutomatic() {
    preferences.setValue(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT, "FOO");
    assertFalse(CloudSdkPreferences.isAutoManaging(preferences));
  }

  @Test
  public void testInitializeDefaults_noChangeIfSet() {
    preferences.setValue(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT, "FOO");
    CloudSdkPreferences.initializeDefaultPreferences(preferences);

    assertEquals("FOO", preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
    assertEquals(
        "AUTOMATIC", preferences.getDefaultString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
  }

  @Test
  public void testConfigure_noCloudSdkAvailable() {
    CloudSdkPreferences.configureManagementPreferences(preferences, false /*cloudSdkAvailable*/);
    assertFalse(preferences.isDefault(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
    assertEquals("AUTOMATIC", preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
  }

  @Test
  public void testConfigure_cloudSdkAvailable() {
    CloudSdkPreferences.configureManagementPreferences(preferences, true /*cloudSdkAvailable*/);
    assertFalse(preferences.isDefault(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
    assertEquals("MANUAL", preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
  }

  @Test
  public void testConfigure_hasCloudSdkPath() {
    preferences.putValue(CloudSdkPreferences.CLOUD_SDK_PATH, "/a/path");

    CloudSdkPreferences.configureManagementPreferences(preferences, false /*cloudSdkAvailable*/);
    assertFalse(preferences.isDefault(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
    assertEquals("MANUAL", preferences.getString(CloudSdkPreferences.CLOUD_SDK_MANAGEMENT));
  }

  @Test
  public void testFlushPreferences() throws BackingStoreException {
    IEclipsePreferences preferencesNode = mock(IEclipsePreferences.class);
    CloudSdkPreferences.flushPreferences(preferencesNode);
    verify(preferencesNode).flush();
  }
}
