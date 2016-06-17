package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.resources.IProject;

import java.io.File;
import java.net.URI;

/**
 * Collects all data needed to create and configure an App Engine Standard Project.
 */
class AppEngineStandardProjectConfig {
  private File cloudSdkLocation = null;
  private URI eclipseProjectLocationUri = null;
  private String appEngineProjectId = "";
  private String packageName = "";
  private IProject project;


  public File getCloudSdkLocation() {
    return cloudSdkLocation;
  }

  public void setCloudSdkLocation(File cloudSdkLocation) {
    this.cloudSdkLocation = cloudSdkLocation;
  }

  // todo does builder pattern make more sense here?
  public void setAppEngineProjectId(String id) {
    this.appEngineProjectId = id;
  }

  public String getAppEngineProjectId() {
    return this.appEngineProjectId;
  }
  
  public void setPackageName(String name) {
    this.packageName = name;
  }

  public String getPackageName() {
    return this.packageName;
  }

  /**
   * Null project location URI means the default location.
   */
  public void setProject(IProject project) {
    this.project = project;
  }

  public IProject getProject() {
    return this.project;
  }

  public URI getEclipseProjectLocationUri() {
    return this.eclipseProjectLocationUri;
  }

  public void setEclipseProjectLocationUri(URI uri) {
    this.eclipseProjectLocationUri = uri;
  }

}
