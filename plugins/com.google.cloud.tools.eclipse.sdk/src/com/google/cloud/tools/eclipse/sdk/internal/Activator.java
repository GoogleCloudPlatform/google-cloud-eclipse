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

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

  private IPreferenceChangeListener listener = new IPreferenceChangeListener() {
    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
      Object newValue = event.getNewValue();
      if (PreferenceConstants.CLOUDSDK_PATH.equals(event.getKey())
          && (newValue == null || newValue instanceof String)) {
        CloudSdkContextFunction.sdkPathChanged((String) newValue);
      }
    }
  };

  @Override
  public void start(BundleContext context) throws Exception {
    PreferenceConstants.getPreferenceNode().addPreferenceChangeListener(listener);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    PreferenceConstants.getPreferenceNode().removePreferenceChangeListener(listener);
  }
}
