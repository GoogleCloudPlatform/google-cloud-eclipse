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

package com.google.cloud.tools.eclipse.usagetracker;

import com.google.common.annotations.VisibleForTesting;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Constants for preference keys associated with analytics reporting.
 */
public class AnalyticsPreferences {
  @VisibleForTesting
  static final String PREFERENCE_PATH = "com.google.cloud.tools.eclipse.usagetracker";

  // See AnalyticsPingManager for the details of these fields.
  public static final String ANALYTICS_OPT_IN = "ANALYTICS_OPT_IN";
  public static final String ANALYTICS_OPT_IN_REGISTERED = "ANALYTICS_OPT_IN_REGISTERED";
  public static final String ANALYTICS_CLIENT_ID = "ANALYTICS_CLIENT_ID";

  static final boolean ANALYTICS_OPT_IN_DEFAULT = false;

  static IPreferenceStore getPreferenceStore() {
    return new ScopedPreferenceStore(ConfigurationScope.INSTANCE, PREFERENCE_PATH);
  }

  static IEclipsePreferences getPreferenceNode() {
    return ConfigurationScope.INSTANCE.getNode(PREFERENCE_PATH);
  }
}
