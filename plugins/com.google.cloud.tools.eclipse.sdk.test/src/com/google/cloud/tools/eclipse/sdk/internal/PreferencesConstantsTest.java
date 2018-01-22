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
import static org.junit.Assert.assertTrue;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.Test;

public class PreferencesConstantsTest {

  @Test
  public void testInitializeDefaults() {
    IPreferenceStore preferences = PreferenceConstants.getPreferenceStore();
    preferences.putValue(PreferenceConstants.CLOUD_SDK_MANAGEMENT, "CUSTOM");
    preferences.setDefault(PreferenceConstants.CLOUD_SDK_AUTO_UPDATE, false);

    PreferenceConstants.initializeDefaults();

    assertTrue(preferences.getDefaultBoolean(PreferenceConstants.CLOUD_SDK_AUTO_UPDATE));
    assertEquals("MANAGED", preferences.getDefaultString(PreferenceConstants.CLOUD_SDK_MANAGEMENT));
  }
}
