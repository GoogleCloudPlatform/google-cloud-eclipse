package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

public class StandardDeployPreferences {

  public static final String PREFERENCE_STORE_DEFAULTS_QUALIFIER =
      "com.google.cloud.tools.eclipse.appengine.deploy.defaults";
  public static final String PREFERENCE_STORE_QUALIFIER = "com.google.cloud.tools.eclipse.appengine.deploy";

  private static final String PREF_PROMPT_FOR_PROJECT_ID = "project.id.promptOnDeploy"; // boolean
  private static final String PREF_PROJECT_ID = "project.id";
  private static final String PREF_OVERRIDE_DEFAULT_VERSIONING = "project.version.overrideDefault"; // boolean
  private static final String PREF_CUSTOM_VERSION = "project.version";
  private static final String PREF_ENABLE_AUTO_PROMOTE = "project.promote"; // boolean
  private static final String PREF_OVERRIDE_DEFAULT_BUCKET = "project.bucket.overrideDefault"; // boolean
  private static final String PREF_CUSTOM_BUCKET = "project.bucket";

  private IEclipsePreferences preferenceStore;

  public StandardDeployPreferences(IProject project) {
    preferenceStore = new ProjectScope(project).getNode(StandardDeployPreferences.PREFERENCE_STORE_QUALIFIER);
  }

  public void save() throws BackingStoreException {
    preferenceStore.flush();
  }

  public boolean isPromptForProjectId() {
    return preferenceStore.getBoolean(PREF_PROMPT_FOR_PROJECT_ID, true);
  }

  public void setPromptForProjectId(boolean promptForProjectId) {
    preferenceStore.putBoolean(PREF_PROMPT_FOR_PROJECT_ID, promptForProjectId);
  }

  public String getProjectId() {
    return preferenceStore.get(PREF_PROJECT_ID, null);
  }

  public void setProjectId(String projectId) {
    preferenceStore.put(PREF_PROJECT_ID, projectId);
  }

  public boolean isOverrideDefaultVersioning() {
    return preferenceStore.getBoolean(PREF_OVERRIDE_DEFAULT_VERSIONING, false);
  }

  public void setOverrideDefaultVersioning(boolean overrideDefaultVersioning) {
    preferenceStore.putBoolean(PREF_OVERRIDE_DEFAULT_VERSIONING, overrideDefaultVersioning);
  }

  public String getVersion() {
    return preferenceStore.get(PREF_CUSTOM_VERSION, null);
  }

  public void setVersion(String version) {
    preferenceStore.put(PREF_CUSTOM_VERSION, version);
  }

  public boolean isAutoPromote() {
    return preferenceStore.getBoolean(PREF_ENABLE_AUTO_PROMOTE, false);
  }

  public void setAutoPromote(boolean autoPromote) {
    preferenceStore.putBoolean(PREF_ENABLE_AUTO_PROMOTE, autoPromote);
  }

  public boolean isOverrideDefaultBucket() {
    return preferenceStore.getBoolean(PREF_OVERRIDE_DEFAULT_BUCKET, false);
  }

  public void setOverrideDefaultBucket(boolean overrideDefaultBucket) {
    preferenceStore.putBoolean(PREF_OVERRIDE_DEFAULT_BUCKET, overrideDefaultBucket);
  }

  public String getBucket() {
    return preferenceStore.get(PREF_CUSTOM_BUCKET, null);
  }

  public void setBucket(String bucket) {
    preferenceStore.put(PREF_CUSTOM_BUCKET, bucket);
  }
}
