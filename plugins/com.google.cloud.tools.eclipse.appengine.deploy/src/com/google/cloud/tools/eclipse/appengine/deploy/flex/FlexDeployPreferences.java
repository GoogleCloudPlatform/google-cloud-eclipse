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

/**
 * Deploy parameters required to deploy an Eclipse project to App Engine flexible. The class adds
 * one more parameter, an {@code app.yaml} path, to the common deploy parameters defined in {@link
 * DeployPrefereces}.
 */
import com.google.cloud.tools.eclipse.appengine.deploy.DeployPreferences;
import com.google.common.base.Strings;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

public class FlexDeployPreferences extends DeployPreferences {

  protected static final String PREF_APP_YAML_PATH = "app.yaml.path";

  private static final String DEFAULT_APP_YAML_PATH = "src/main/appengine/app.yaml";

  private String appYamlPath;

  public FlexDeployPreferences(IProject project) {
    this(new ProjectScope(project).getNode(PREFERENCE_STORE_QUALIFIER));
  }

  protected FlexDeployPreferences(IEclipsePreferences preferenceStore) {
    super(preferenceStore);
    appYamlPath = preferenceStore.get(PREF_APP_YAML_PATH, DEFAULT_APP_YAML_PATH);
  }

  @Override
  public void resetToDefaults() {
    super.resetToDefaults();
    appYamlPath = DEFAULT_APP_YAML_PATH;
  }

  @Override
  public void save() throws BackingStoreException {
    preferenceStore.put(PREF_APP_YAML_PATH, appYamlPath);
    super.save();
  }

  public String getAppYamlPath() {
    return appYamlPath;
  }

  public void setAppYamlPath(String appYamlPath) {
    this.appYamlPath = Strings.nullToEmpty(appYamlPath);
  }

}
