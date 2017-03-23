/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.preferences;

import com.google.cloud.tools.eclipse.dataflow.core.DataflowCorePlugin;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 * A {@link PreferenceStore} backed by an {@code IEclipsePreferences}.
 */
class EclipsePreferenceStore implements PreferenceStore {
  private final IEclipsePreferences preferences;

  public EclipsePreferenceStore(IEclipsePreferences eclipsePreferences) {
    this.preferences = eclipsePreferences;
  }

  @Override
  public String getOption(String name) {
    return preferences.get(name, null);
  }

  @Override
  public void setOption(String name, String value) {
    preferences.put(name, value);
  }

  @Override
  public void save() {
    try {
      preferences.flush();
    } catch (BackingStoreException e) {
      DataflowCorePlugin.logError(e, "Failed to flush Dataflow preferences");
    }
  }
}

