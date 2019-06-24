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

import com.google.common.base.Strings;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Common deploy parameters (e.g., account email, GCP project ID, whether to stop previous versions,
 * etc.) required to deploy an Eclipse project to App Engine. The parameters are saved to a
 * project-scoped preferences store.
 *
 * Incidentally, these parameters are all that the standard deploy needs.
 */
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
  static final String PREF_ALLOW_OBSOLETE_RUNTIME = "project.allowObsoleteRuntime"; // boolean

  public static final String DEFAULT_ACCOUNT_EMAIL = "";
  public static final String DEFAULT_PROJECT_ID = "";
  public static final String DEFAULT_CUSTOM_VERSION = "";
  public static final boolean DEFAULT_ENABLE_AUTO_PROMOTE = true;
  public static final boolean DEFAULT_INCLUDE_OPTIONAL_CONFIGURATION_FILES = true;
  public static final String DEFAULT_CUSTOM_BUCKET = "";
  public static final boolean DEFAULT_STOP_PREVIOUS_VERSION = true;
  public static final boolean DEFAULT_ALLOW_OBSOLETE_RUNTIME = false;

  protected final IEclipsePreferences preferenceStore;

  private String accountEmail;
  private String projectId;
  private String version;
  private boolean autoPromote;
  private boolean stopPreviousVersion;
  private boolean includeOptionalConfigurationFiles;
  private String bucket;
  private boolean allowObsoleteRuntime;

  public DeployPreferences(IProject project) {
    this(new ProjectScope(project).getNode(PREFERENCE_STORE_QUALIFIER));
  }

  protected DeployPreferences(IEclipsePreferences preferenceStore) {
    this.preferenceStore = preferenceStore;

    accountEmail = preferenceStore.get(PREF_ACCOUNT_EMAIL, DEFAULT_ACCOUNT_EMAIL);
    projectId = preferenceStore.get(PREF_PROJECT_ID, DEFAULT_PROJECT_ID);
    version = preferenceStore.get(PREF_CUSTOM_VERSION, DEFAULT_CUSTOM_VERSION);
    autoPromote = preferenceStore.getBoolean(PREF_ENABLE_AUTO_PROMOTE, DEFAULT_ENABLE_AUTO_PROMOTE);
    includeOptionalConfigurationFiles = preferenceStore.getBoolean(
        PREF_INCLUDE_OPTIONAL_CONFIGURATION_FILES, DEFAULT_INCLUDE_OPTIONAL_CONFIGURATION_FILES);
    bucket = preferenceStore.get(PREF_CUSTOM_BUCKET, DEFAULT_CUSTOM_BUCKET);
    stopPreviousVersion = preferenceStore.getBoolean(
        PREF_STOP_PREVIOUS_VERSION, DEFAULT_STOP_PREVIOUS_VERSION);
    allowObsoleteRuntime =
        preferenceStore.getBoolean(PREF_ALLOW_OBSOLETE_RUNTIME, DEFAULT_ALLOW_OBSOLETE_RUNTIME);
  }

  public void resetToDefaults() {
    accountEmail = DEFAULT_ACCOUNT_EMAIL;
    projectId = DEFAULT_PROJECT_ID;
    version = DEFAULT_CUSTOM_VERSION;
    autoPromote = DEFAULT_ENABLE_AUTO_PROMOTE;
    stopPreviousVersion = DEFAULT_STOP_PREVIOUS_VERSION;
    includeOptionalConfigurationFiles = DEFAULT_INCLUDE_OPTIONAL_CONFIGURATION_FILES;
    bucket = DEFAULT_CUSTOM_BUCKET;
    allowObsoleteRuntime = DEFAULT_ALLOW_OBSOLETE_RUNTIME;
  }

  public void save() throws BackingStoreException {
    preferenceStore.put(PREF_ACCOUNT_EMAIL, Strings.nullToEmpty(accountEmail));
    preferenceStore.put(PREF_PROJECT_ID, Strings.nullToEmpty(projectId));
    preferenceStore.put(PREF_CUSTOM_VERSION, Strings.nullToEmpty(version));
    preferenceStore.putBoolean(PREF_ENABLE_AUTO_PROMOTE, autoPromote);
    preferenceStore.putBoolean(
        PREF_INCLUDE_OPTIONAL_CONFIGURATION_FILES, includeOptionalConfigurationFiles);
    preferenceStore.put(PREF_CUSTOM_BUCKET, Strings.nullToEmpty(bucket));
    preferenceStore.putBoolean(PREF_STOP_PREVIOUS_VERSION, stopPreviousVersion);
    preferenceStore.putBoolean(PREF_ALLOW_OBSOLETE_RUNTIME, allowObsoleteRuntime);
    preferenceStore.flush();
  }

  public String getAccountEmail() {
    return accountEmail;
  }

  public void setAccountEmail(String accountEmail) {
    this.accountEmail = Strings.nullToEmpty(accountEmail);
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = Strings.nullToEmpty(projectId);
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = Strings.nullToEmpty(version);
  }

  public boolean isAutoPromote() {
    return autoPromote;
  }

  public void setAutoPromote(boolean autoPromote) {
    this.autoPromote = autoPromote;
  }

  public boolean isIncludeOptionalConfigurationFiles() {
    return includeOptionalConfigurationFiles;
  }

  public void setIncludeOptionalConfigurationFiles(boolean includeOptionalConfigurationFiles) {
    this.includeOptionalConfigurationFiles = includeOptionalConfigurationFiles;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = Strings.nullToEmpty(bucket);
  }

  public boolean isStopPreviousVersion() {
    return stopPreviousVersion;
  }

  public void setStopPreviousVersion(boolean stopPreviousVersion) {
    this.stopPreviousVersion = stopPreviousVersion;
  }

  /** Return {@code true} if this project can deploy to an obsolete runtime. */
  public boolean getAllowObsoleteRuntime() {
    return allowObsoleteRuntime;
  }

  public void setAllowObsoleteRuntime(boolean allowObsoleteRuntime) {
    this.allowObsoleteRuntime = allowObsoleteRuntime;
  }
}
