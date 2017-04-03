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

package com.google.cloud.tools.eclipse.appengine.deploy.flex;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class FlexDeployPreferenceInitializer extends AbstractPreferenceInitializer {
  private static final String PREFERENCE_STORE_DEFAULTS_QUALIFIER =
      "com.google.cloud.tools.eclipse.appengine.deploy.flex.defaults";

  static final String DEFAULT_APP_ENGINE_DIRECTORY = "src/main/appengine";
  static final String DEFAULT_DOCKER_DIRECTORY = "";
  static final boolean DEFAULT_USE_DEPLOYMENT_PREFERENCES = false;

  @Override
  public void initializeDefaultPreferences() {
    IEclipsePreferences preferences =
        DefaultScope.INSTANCE.getNode(PREFERENCE_STORE_DEFAULTS_QUALIFIER);
    preferences.put(FlexDeployPreferences.PREF_APP_ENGINE_DIRECTORY, DEFAULT_APP_ENGINE_DIRECTORY);
    preferences.put(FlexDeployPreferences.PREF_DOCKER_DIRECTORY, DEFAULT_DOCKER_DIRECTORY);
    preferences.putBoolean(FlexDeployPreferences.PREF_USE_DEPLOYMENT_PREFERENCES,
        DEFAULT_USE_DEPLOYMENT_PREFERENCES);
  }

  public static IEclipsePreferences getDefaultPreferences() {
    return DefaultScope.INSTANCE.getNode(PREFERENCE_STORE_DEFAULTS_QUALIFIER);
  }

}
