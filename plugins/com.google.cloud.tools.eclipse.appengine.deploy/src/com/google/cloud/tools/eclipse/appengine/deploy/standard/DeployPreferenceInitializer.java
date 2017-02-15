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

package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class DeployPreferenceInitializer extends AbstractPreferenceInitializer {

  private static final String PREFERENCE_STORE_DEFAULTS_QUALIFIER =
      "com.google.cloud.tools.eclipse.appengine.deploy.defaults";

  static final String DEFAULT_ACCOUNT_EMAIL = "";
  static final String DEFAULT_PROJECT_ID = "";
  static final boolean DEFAULT_OVERRIDE_DEFAULT_VERSIONING = false;
  static final String DEFAULT_CUSTOM_VERSION = "";
  static final boolean DEFAULT_ENABLE_AUTO_PROMOTE = true;
  static final boolean DEFAULT_OVERRIDE_DEFAULT_BUCKET = false;
  static final String DEFAULT_CUSTOM_BUCKET = "";
  static final boolean DEFAULT_STOP_PREVIOUS_VERSION = true;

  @Override
  public void initializeDefaultPreferences() {
    IEclipsePreferences preferences =
        DefaultScope.INSTANCE.getNode(PREFERENCE_STORE_DEFAULTS_QUALIFIER);
    preferences.put(StandardDeployPreferences.PREF_ACCOUNT_EMAIL, DEFAULT_ACCOUNT_EMAIL);
    preferences.put(StandardDeployPreferences.PREF_PROJECT_ID,
                    DEFAULT_PROJECT_ID);
    preferences.putBoolean(StandardDeployPreferences.PREF_OVERRIDE_DEFAULT_VERSIONING,
                           DEFAULT_OVERRIDE_DEFAULT_VERSIONING);
    preferences.put(StandardDeployPreferences.PREF_CUSTOM_VERSION,
                    DEFAULT_CUSTOM_VERSION);
    preferences.putBoolean(StandardDeployPreferences.PREF_ENABLE_AUTO_PROMOTE,
                           DEFAULT_ENABLE_AUTO_PROMOTE);
    preferences.putBoolean(StandardDeployPreferences.PREF_OVERRIDE_DEFAULT_BUCKET,
                           DEFAULT_OVERRIDE_DEFAULT_BUCKET);
    preferences.put(StandardDeployPreferences.PREF_CUSTOM_BUCKET,
                    DEFAULT_CUSTOM_BUCKET);
    preferences.putBoolean(StandardDeployPreferences.PREF_STOP_PREVIOUS_VERSION,
                    DEFAULT_STOP_PREVIOUS_VERSION);
  }

  public static IEclipsePreferences getDefaultPreferences() {
    return DefaultScope.INSTANCE.getNode(PREFERENCE_STORE_DEFAULTS_QUALIFIER);
  }
}
