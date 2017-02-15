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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployPreferences;
import org.eclipse.core.resources.IProject;
import org.osgi.service.prefs.BackingStoreException;

public class DeployPreferencesModel {

  private StandardDeployPreferences preferences;

  private String accountEmail;
  private String projectId;
  private String version;
  private boolean autoPromote;
  private boolean stopPreviousVersion;
  private String bucket;

  public DeployPreferencesModel(IProject project) {
    preferences = new StandardDeployPreferences(project);
    applyPreferences(preferences);
  }

  private void applyPreferences(StandardDeployPreferences preferences) {
    setAccountEmail(preferences.getAccountEmail());
    setProjectId(preferences.getProjectId());
    setVersion(preferences.getVersion());
    setAutoPromote(preferences.isAutoPromote());
    setStopPreviousVersion(preferences.isStopPreviousVersion());
    setBucket(preferences.getBucket());
  }

  void resetToDefaults() {
    applyPreferences(StandardDeployPreferences.getDefaultPreferences());
  }

  public void savePreferences() throws BackingStoreException {
    preferences.setAccountEmail(getAccountEmail());
    preferences.setProjectId(getProjectId());
    preferences.setVersion(getVersion());
    preferences.setAutoPromote(isAutoPromote());
    preferences.setStopPreviousVersion(isStopPreviousVersion());
    preferences.setBucket(getBucket());
    preferences.save();
  }

  public String getAccountEmail() {
    return accountEmail;
  }

  public void setAccountEmail(String accountEmail) {
    this.accountEmail = accountEmail;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public boolean isAutoPromote() {
    return autoPromote;
  }

  public void setAutoPromote(boolean autoPromote) {
    this.autoPromote = autoPromote;
  }

  public boolean isStopPreviousVersion() {
    return stopPreviousVersion;
  }

  public void setStopPreviousVersion(boolean stopPreviousVersion) {
    this.stopPreviousVersion = stopPreviousVersion;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }
}
