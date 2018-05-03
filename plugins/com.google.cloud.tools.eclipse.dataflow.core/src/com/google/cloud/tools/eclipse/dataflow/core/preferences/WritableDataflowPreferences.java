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
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IProject;

/**
 * An implementation of {@link DataflowPreferences} that references methods of an underlying {@link
 * PreferenceStore} and allows writing the preferences.
 */
public class WritableDataflowPreferences implements DataflowPreferences {
  private final PreferenceStore preferenceStore;

  /**
   * Returns the {@link DataflowPreferences} for the workspace.
   */
  public static WritableDataflowPreferences global() {
    return new WritableDataflowPreferences(
        new EclipsePreferenceStore(DataflowCorePlugin.getDefault().getPreferences()));
  }

  /**
   * Returns the {@link DataflowPreferences} backed by the provided project.
   */
  public static WritableDataflowPreferences forProject(IProject project) {
    return new WritableDataflowPreferences(new ProjectPreferenceStore(project));
  }

  private WritableDataflowPreferences(PreferenceStore preferenceStore) {
    this.preferenceStore = preferenceStore;
  }

  public void setDefaultAccountEmail(String accountEmail) {
    preferenceStore.setOption(ACCOUNT_EMAIL_PROPERTY, accountEmail);
  }

  @Override
  public String getDefaultAccountEmail() {
    return preferenceStore.getOption(ACCOUNT_EMAIL_PROPERTY);
  }

  public void setDefaultProject(String project) {
    preferenceStore.setOption(PROJECT_PROPERTY, project);
  }

  @Override
  public String getDefaultProject() {
    return preferenceStore.getOption(PROJECT_PROPERTY);
  }

  public void setDefaultStagingLocation(String stagingLocation) {
    preferenceStore.setOption(STAGING_LOCATION_PROPERTY, stagingLocation);
  }

  @Override
  public String getDefaultStagingLocation() {
    return preferenceStore.getOption(STAGING_LOCATION_PROPERTY);
  }

  @Override
  public String getDefaultGcpTempLocation() {
    return preferenceStore.getOption(STAGING_LOCATION_PROPERTY);
  }

  public void setDefaultServiceAccountKey(String key) {
    preferenceStore.setOption(SERVICE_ACCOUNT_KEY_PROPERTY, key);
  }

  @Override
  public String getDefaultServiceAccountKey() {
    return preferenceStore.getOption(SERVICE_ACCOUNT_KEY_PROPERTY);
  }

  public void save() {
    preferenceStore.save();
  }

  @Override
  public Map<String, String> asDefaultPropertyMap() {
    Map<String, String> result = new HashMap<>();
    String defaultAccountEmail = getDefaultAccountEmail();
    if (defaultAccountEmail != null) {
      result.put(ACCOUNT_EMAIL_PROPERTY, defaultAccountEmail);
    }

    String defaultProject = getDefaultProject();
    if (defaultProject != null) {
      result.put(PROJECT_PROPERTY, defaultProject);
    }

    String defaultStagingLocation = getDefaultStagingLocation();
    if (defaultStagingLocation != null) {
      result.put(STAGING_LOCATION_PROPERTY, defaultStagingLocation);
      result.put(GCP_TEMP_LOCATION_PROPERTY, defaultStagingLocation);
    }

    String defaultKey = getDefaultServiceAccountKey();
    if (defaultKey != null) {
      result.put(SERVICE_ACCOUNT_KEY_PROPERTY, defaultKey);
    }
    return result;
  }
}
