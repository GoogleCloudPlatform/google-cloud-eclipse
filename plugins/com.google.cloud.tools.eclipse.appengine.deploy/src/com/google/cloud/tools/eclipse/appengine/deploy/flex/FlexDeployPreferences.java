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

import com.google.cloud.tools.eclipse.appengine.deploy.DeployPreferences;
import com.google.common.base.Strings;
import org.eclipse.core.resources.IProject;
import org.osgi.service.prefs.BackingStoreException;

public class FlexDeployPreferences extends DeployPreferences {

  static final String PREF_APP_ENGINE_DIRECTORY = "appengine.config.folder";

  public static final String DEFAULT_APP_ENGINE_DIRECTORY = "src/main/appengine";

  private String appEngineDirectory;

  public FlexDeployPreferences(IProject project) {
    super(project);
    appEngineDirectory =
        preferenceStore.get(PREF_APP_ENGINE_DIRECTORY, DEFAULT_APP_ENGINE_DIRECTORY);
  }

  @Override
  public void resetToDefaults() {
    appEngineDirectory = DEFAULT_APP_ENGINE_DIRECTORY;
    super.resetToDefaults();
  }

  @Override
  public void save() throws BackingStoreException {
    preferenceStore.put(PREF_APP_ENGINE_DIRECTORY, appEngineDirectory);
    super.save();
  }

  public String getAppEngineDirectory() {
    return appEngineDirectory;
  }

  public void setAppEngineDirectory(String appEngineDirectory) {
    this.appEngineDirectory = Strings.nullToEmpty(appEngineDirectory);
  }

}
