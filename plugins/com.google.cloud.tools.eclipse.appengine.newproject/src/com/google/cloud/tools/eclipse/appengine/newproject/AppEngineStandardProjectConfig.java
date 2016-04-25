package com.google.cloud.tools.eclipse.appengine.newproject;

/**
 * Collects all data needed to create and configure an App Engine Standard Project.
 */
class AppEngineStandardProjectConfig {

  private String appEngineProjectId;
  private String eclipseProjectName;
  private String packageName;
  private String eclipseProjectDirectory;

  // todo does builder pattern make more sense here?
  public void setAppEngineProjectId(String id) {
    this.appEngineProjectId = id;
  }

  public String getAppEngineProjectId() {
    return this.appEngineProjectId;
  }
  
  public void setEclipseProjectName(String name) {
    this.eclipseProjectName = name;
  }

  public String getEclipseProjectName() {
    return this.eclipseProjectName;
  }
  
  public void setPackageName(String name) {
    this.packageName = name;
  }
  
  public void setEclipseProjectDirectory(String path) {
    this.eclipseProjectDirectory = path;
  }
  public String getEclipseProjectDirectory() {
    return this.eclipseProjectDirectory;
  }

  public String getPackageName() {
    return this.packageName;
  }

}
