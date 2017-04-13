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

package com.google.cloud.tools.eclipse.appengine.deploy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

public class DeployPreferences {

  public static final String PREFERENCE_STORE_QUALIFIER =
      "com.google.cloud.tools.eclipse.appengine.deploy";

  static final String PREF_ACCOUNT_EMAIL = "account.email";
  static final String PREF_PROJECT_ID = "project.id";
  static final String PREF_CUSTOM_VERSION = "project.version";
  static final String PREF_ENABLE_AUTO_PROMOTE = "project.promote"; // boolean
  static final String PREF_INCLUDE_OPTIONAL_CONFIGURATION_FILES =
      "include.optional.configuration.files"; // boolean
  static final String PREF_CUSTOM_BUCKET = "project.bucket";
  static final String PREF_STOP_PREVIOUS_VERSION = "project.previousVersion.stop"; // boolean

  private final IEclipsePreferences preferenceStore;

  public static DeployPreferences getDefaultPreferences() {
    return new DeployPreferences(DeployPreferenceInitializer.getDefaultPreferences());
  }

  public DeployPreferences(IProject project) {
    this(new ProjectScope(project).getNode(PREFERENCE_STORE_QUALIFIER));
  }

  @VisibleForTesting
  DeployPreferences(IEclipsePreferences preferences) {
    preferenceStore = preferences;
  }

  public void save() throws BackingStoreException {
    preferenceStore.flush();
  }

  public String getAccountEmail() {
    return preferenceStore.get(PREF_ACCOUNT_EMAIL,
                               DeployPreferenceInitializer.DEFAULT_ACCOUNT_EMAIL);
  }

  public void setAccountEmail(String accountEmail) {
    preferenceStore.put(PREF_ACCOUNT_EMAIL, Strings.nullToEmpty(accountEmail));
  }

  public String getProjectId() {
    return preferenceStore.get(PREF_PROJECT_ID, DeployPreferenceInitializer.DEFAULT_PROJECT_ID);
  }

  public void setProjectId(String projectId) {
    preferenceStore.put(PREF_PROJECT_ID, Strings.nullToEmpty(projectId));
  }

  public String getVersion() {
    return preferenceStore.get(PREF_CUSTOM_VERSION,
                               DeployPreferenceInitializer.DEFAULT_CUSTOM_VERSION);
  }

  public void setVersion(String version) {
    preferenceStore.put(PREF_CUSTOM_VERSION, Strings.nullToEmpty(version));
  }

  public boolean isAutoPromote() {
    return preferenceStore.getBoolean(PREF_ENABLE_AUTO_PROMOTE,
                                      DeployPreferenceInitializer.DEFAULT_ENABLE_AUTO_PROMOTE);
  }

  public void setAutoPromote(boolean autoPromote) {
    preferenceStore.putBoolean(PREF_ENABLE_AUTO_PROMOTE, autoPromote);
  }

  public boolean isIncludeOptionalConfigurationFiles() {
    return preferenceStore.getBoolean(PREF_INCLUDE_OPTIONAL_CONFIGURATION_FILES,
        DeployPreferenceInitializer.DEFAULT_INCLUDE_OPTIONAL_CONFIGURATION_FILES);
  }

  public void setIncludeOptionalConfigurationFiles(boolean includeOptionalConfigurationFiles) {
    preferenceStore.putBoolean(
        PREF_INCLUDE_OPTIONAL_CONFIGURATION_FILES, includeOptionalConfigurationFiles);
  }

  public String getBucket() {
    return preferenceStore.get(PREF_CUSTOM_BUCKET,
                               DeployPreferenceInitializer.DEFAULT_CUSTOM_BUCKET);
  }

  public void setBucket(String bucket) {
    preferenceStore.put(PREF_CUSTOM_BUCKET, Strings.nullToEmpty(bucket));
  }

  public boolean isStopPreviousVersion() {
    return preferenceStore.getBoolean(PREF_STOP_PREVIOUS_VERSION,
                                      DeployPreferenceInitializer.DEFAULT_STOP_PREVIOUS_VERSION);
  }

  public void setStopPreviousVersion(boolean stopPreviousVersion) {
    preferenceStore.putBoolean(PREF_STOP_PREVIOUS_VERSION, stopPreviousVersion);
  }

}
