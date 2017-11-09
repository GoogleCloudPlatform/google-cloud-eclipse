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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Constant definitions for plug-in preferences.
 */
public final class PreferenceConstants {
  // host bundle for the preference
  static final String BUNDLEID = "com.google.cloud.tools.eclipse.sdk";

  /**
   * Preference name for the path to the Google Cloud SDK.
   */
  public static final String CLOUDSDK_PATH = "cloudSdkPath";

  static IPreferenceStore getPreferenceStore() {
    return new ScopedPreferenceStore(InstanceScope.INSTANCE, BUNDLEID);
  }

  static IEclipsePreferences getPreferenceNode() {
    return InstanceScope.INSTANCE.getNode(BUNDLEID);
  }
}
