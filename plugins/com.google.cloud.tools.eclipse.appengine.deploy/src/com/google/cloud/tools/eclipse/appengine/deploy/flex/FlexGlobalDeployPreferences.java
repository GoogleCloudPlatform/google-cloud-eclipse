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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

public class FlexGlobalDeployPreferences extends FlexDeployPreferences {

  @VisibleForTesting
  static final String PREF_DEPLOY_ARTIFACT_PATH = "deploy.artifact.path";

  private static final String DEFAULT_APP_YAML_PATH = "app.yaml";
  private static final String DEFAULT_DEPLOY_ARTIFACT_PATH = "";

  private String deployArtifactPath;

  public FlexGlobalDeployPreferences() {
    super(InstanceScope.INSTANCE.getNode(PREFERENCE_STORE_QUALIFIER));
    deployArtifactPath = preferenceStore.get(PREF_DEPLOY_ARTIFACT_PATH,
        DEFAULT_DEPLOY_ARTIFACT_PATH);
    setAppYamlPath(preferenceStore.get(PREF_APP_YAML_PATH, DEFAULT_APP_YAML_PATH));
  }

  @Override
  public void resetToDefaults() {
    super.resetToDefaults();
    deployArtifactPath = DEFAULT_DEPLOY_ARTIFACT_PATH;
    setAppYamlPath(DEFAULT_APP_YAML_PATH);
  }

  @Override
  public void save() throws BackingStoreException {
    preferenceStore.put(PREF_DEPLOY_ARTIFACT_PATH, deployArtifactPath);
    super.save();
  }

  public String getDeployArtifactPath() {
    return deployArtifactPath;
  }

  public void setDeployArtifactPath(String deployArtifactPath) {
    this.deployArtifactPath = Strings.nullToEmpty(deployArtifactPath);
  }
}