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

package com.google.cloud.tools.eclipse.sdk.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Class for Cloud SDK preferences: defining constants, accessing their preference store and node,
 * initializing default values, etc.
 */
public final class CloudSdkPreferences extends AbstractPreferenceInitializer {
  // host bundle for the preference
  static final String BUNDLEID = "com.google.cloud.tools.eclipse.sdk";

  public static enum CloudSdkManagementOption {
    AUTOMATIC, MANUAL
  }

  /**
   * Preference name for how the Google Cloud SDK is managed. Actual values are the names (strings)
   * of the {@code enum CloudSdkManagementOption} constants.
   *
   * @see CloudSdkManagementOption#name()
   */
  public static final String CLOUD_SDK_MANAGEMENT = "cloudSdkManagement";

  /**
   * Preference name for the path to the Google Cloud SDK.
   */
  public static final String CLOUD_SDK_PATH = "cloudSdkPath";

  static IPreferenceStore getPreferenceStore() {
    return new ScopedPreferenceStore(InstanceScope.INSTANCE, BUNDLEID);
  }

  static IEclipsePreferences getPreferenceNode() {
    return InstanceScope.INSTANCE.getNode(BUNDLEID);
  }

  @Override
  public void initializeDefaultPreferences() {
    getPreferenceStore().setDefault(CLOUD_SDK_MANAGEMENT,
        CloudSdkManagementOption.AUTOMATIC.name());
  }
}
