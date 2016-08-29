package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.core.resources.IProject;
import org.osgi.service.prefs.BackingStoreException;

import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployPreferences;

public class DeployPreferencesModel {

  private StandardDeployPreferences preferences;

  private boolean promptForProjectId;
  private String projectId;
  private boolean overrideDefaultVersioning;
  private String version;
  private boolean autoPromote;
  private boolean overrideDefaultBucket;
  private String bucket;

  public DeployPreferencesModel(IProject project) {
    preferences = new StandardDeployPreferences(project);
    applyPreferences(preferences);
  }

  private void applyPreferences(StandardDeployPreferences preferences) {
    setProjectId(preferences.getProjectId());
    setPromptForProjectId(preferences.isPromptForProjectId());
    setOverrideDefaultVersioning(preferences.isOverrideDefaultVersioning());
    setVersion(preferences.getVersion());
    setAutoPromote(preferences.isAutoPromote());
    setOverrideDefaultBucket(preferences.isOverrideDefaultBucket());
    setBucket(preferences.getBucket());
  }

  public void resetToDefaults() {
    applyPreferences(StandardDeployPreferences.DEFAULT);
  }

  public void savePreferences() throws BackingStoreException {
    preferences.setProjectId(getProjectId());
    preferences.setPromptForProjectId(isPromptForProjectId());
    preferences.setOverrideDefaultVersioning(isOverrideDefaultVersioning());
    preferences.setVersion(getVersion());
    preferences.setAutoPromote(isAutoPromote());
    preferences.setOverrideDefaultBucket(isOverrideDefaultBucket());
    preferences.setBucket(getBucket());
    preferences.save();
  }

  public boolean isPromptForProjectId() {
    return promptForProjectId;
  }

  public void setPromptForProjectId(boolean promptForProjectId) {
    this.promptForProjectId = promptForProjectId;
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
