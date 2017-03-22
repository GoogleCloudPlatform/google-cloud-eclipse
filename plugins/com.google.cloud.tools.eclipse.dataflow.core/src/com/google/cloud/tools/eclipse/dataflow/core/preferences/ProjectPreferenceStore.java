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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link PreferenceStore} with an {@code IProject} as the backing store.
 */
class ProjectPreferenceStore implements PreferenceStore {
  private final IProject project;

  private Map<String, String> prefsToSave;

  public ProjectPreferenceStore(IProject project) {
    this.project = project;
    this.prefsToSave = new HashMap<>();
  }

  @Override
  public String getOption(String name) {
    if (prefsToSave.containsKey(name)) {
      return prefsToSave.get(name);
    }
    try {
      return project.getPersistentProperty(key(name));
    } catch (CoreException e) {
      return null;
    }
  }

  @Override
  public void setOption(String name, String value) {
    prefsToSave.put(name, value);
  }

  @Override
  public void save() {
    for (Map.Entry<String, String> entryToSave : prefsToSave.entrySet()) {
      String name = entryToSave.getKey();
      String value = entryToSave.getValue();
      try {
        project.setPersistentProperty(key(name), value);
      } catch (CoreException e) {
        DataflowCorePlugin.logError(e,
            "Error while attempting to set Dataflow Preference %s to %s in project %s", name, value,
            project.getName());
      }
    }
    prefsToSave = new HashMap<>();
  }

  private QualifiedName key(String name) {
    return new QualifiedName(DataflowCorePlugin.PLUGIN_ID, name);
  }
}
