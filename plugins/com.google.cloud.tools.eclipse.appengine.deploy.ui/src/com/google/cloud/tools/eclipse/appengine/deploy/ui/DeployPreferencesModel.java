package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.resources.IProject;
import org.osgi.service.prefs.BackingStoreException;

import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployPreferences;

public class DeployPreferencesModel {

  private WritableValue promptForProjectId = new WritableValue(Boolean.TRUE, Boolean.class);
  private WritableValue projectId = new WritableValue();
  private WritableValue overrideDefaultVersioning = new WritableValue(Boolean.FALSE, Boolean.class);
  private WritableValue version = new WritableValue();
  private WritableValue autoPromote = new WritableValue(Boolean.TRUE, Boolean.class);
  private WritableValue overrideDefaultBucket = new WritableValue(Boolean.FALSE, Boolean.class);
  private WritableValue bucket = new WritableValue();

  private StandardDeployPreferences preferences;

  public DeployPreferencesModel(IProject project) {
    preferences = new StandardDeployPreferences(project);
    loadPreferences();
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

  private void loadPreferences() {
    setProjectId(preferences.getProjectId());
    setPromptForProjectId(preferences.isPromptForProjectId());
    setOverrideDefaultVersioning(preferences.isOverrideDefaultVersioning());
    setVersion(preferences.getVersion());
    setAutoPromote(preferences.isAutoPromote());
    setOverrideDefaultBucket(preferences.isOverrideDefaultBucket());
    setBucket(preferences.getBucket());
  }

  public boolean isPromptForProjectId() {
    return (boolean) promptForProjectId.getValue();
  }

  public void setPromptForProjectId(boolean promptForProjectId) {
    this.promptForProjectId.setValue(promptForProjectId);
  }

  public String getProjectId() {
    return (String) projectId.getValue();
  }

  public void setProjectId(String projectId) {
    this.projectId.setValue(projectId);
  }

  public boolean isOverrideDefaultVersioning() {
    return (boolean) overrideDefaultVersioning.getValue();
  }

  public void setOverrideDefaultVersioning(boolean overrideDefaultVersioning) {
    this.overrideDefaultVersioning.setValue(overrideDefaultVersioning);
  }

  public String getVersion() {
    return (String) version.getValue();
  }

  public void setVersion(String version) {
    this.version.setValue(version);
  }

  public boolean isAutoPromote() {
    return (boolean) autoPromote.getValue();
  }

  public void setAutoPromote(boolean autoPromote) {
    this.autoPromote.setValue(autoPromote);
  }

  public boolean isOverrideDefaultBucket() {
    return (boolean) overrideDefaultBucket.getValue();
  }

  public void setOverrideDefaultBucket(boolean overrideDefaultBucket) {
    this.overrideDefaultBucket.setValue(overrideDefaultBucket);
  }

  public String getBucket() {
    return (String) bucket.getValue();
  }

  public void setBucket(String bucket) {
    this.bucket.setValue(bucket);
  }

  public WritableValue observablePromptForProjectId() {
    return promptForProjectId;
  }

  public WritableValue observableProjectId() {
    return projectId;
  }

  public WritableValue observableOverrideDefaultVersioning() {
    return overrideDefaultVersioning;
  }

  public WritableValue observableVersion() {
    return version;
  }

  public WritableValue observableAutoPromote() {
    return autoPromote;
  }

  public WritableValue observableOverrideDefaultBucket() {
    return overrideDefaultBucket;
  }

  public WritableValue observableBucket() {
    return bucket;
  }
}
