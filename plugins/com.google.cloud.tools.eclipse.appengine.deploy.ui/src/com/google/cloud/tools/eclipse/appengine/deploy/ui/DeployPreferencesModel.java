/*******************************************************************************
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
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployPreferences;
import com.google.common.annotations.VisibleForTesting;

import org.eclipse.core.resources.IProject;
import org.osgi.service.prefs.BackingStoreException;

public class DeployPreferencesModel {

  // After the Account Selector is set up with a correct email, we set the email value in this
  // model to this special value. By doing this, we can always force dialog validation whenever
  // an email value in the Account Selector is changed.
  public static final String REVALIDATION_TRICK_EMAIL_VALUE = "Marker: don't save this value";

  private StandardDeployPreferences preferences;

  private String accountEmail;
  private String projectId;
  private boolean overrideDefaultVersioning;
  private String version;
  private boolean autoPromote;
  private boolean stopPreviousVersion;
  private boolean overrideDefaultBucket;
  private String bucket;

  public DeployPreferencesModel(IProject project) {
    this(new StandardDeployPreferences(project));
  }

  @VisibleForTesting
  DeployPreferencesModel(StandardDeployPreferences preferences) {
    this.preferences = preferences;
    applyPreferences(preferences);
  }

  private void applyPreferences(StandardDeployPreferences preferences) {
    setAccountEmail(preferences.getAccountEmail());
    setProjectId(preferences.getProjectId());
    setOverrideDefaultVersioning(preferences.isOverrideDefaultVersioning());
    setVersion(preferences.getVersion());
    setAutoPromote(preferences.isAutoPromote());
    setStopPreviousVersion(preferences.isStopPreviousVersion());
    setOverrideDefaultBucket(preferences.isOverrideDefaultBucket());
    setBucket(preferences.getBucket());
  }

  public void resetToDefaults() {
    applyPreferences(StandardDeployPreferences.DEFAULT);
  }

  public void savePreferences() throws BackingStoreException {
    if (!REVALIDATION_TRICK_EMAIL_VALUE.equals(getAccountEmail())) {
      preferences.setAccountEmail(getAccountEmail());
    }
    preferences.setProjectId(getProjectId());
    preferences.setOverrideDefaultVersioning(isOverrideDefaultVersioning());
    preferences.setVersion(getVersion());
    preferences.setAutoPromote(isAutoPromote());
    preferences.setStopPreviousVersion(isStopPreviousVersion());
    preferences.setOverrideDefaultBucket(isOverrideDefaultBucket());
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

  public boolean isOverrideDefaultVersioning() {
    return overrideDefaultVersioning;
  }

  public void setOverrideDefaultVersioning(boolean overrideDefaultVersioning) {
    this.overrideDefaultVersioning = overrideDefaultVersioning;
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

  public boolean isOverrideDefaultBucket() {
    return overrideDefaultBucket;
  }

  public void setOverrideDefaultBucket(boolean overrideDefaultBucket) {
    this.overrideDefaultBucket = overrideDefaultBucket;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }
}
