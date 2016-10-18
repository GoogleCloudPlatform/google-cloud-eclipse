package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.resources.IProject;

import com.google.cloud.tools.eclipse.appengine.libraries.Library;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Collects all data needed to create and configure an App Engine Standard Project.
 */
class AppEngineStandardProjectConfig {
  private File cloudSdkLocation = null;
  private URI eclipseProjectLocationUri = null;
  private String packageName = "";
  private IProject project;
  private List<Library> appEngineLibraries = Collections.emptyList();


  public File getCloudSdkLocation() {
    return cloudSdkLocation;
  }

  public void setCloudSdkLocation(File cloudSdkLocation) {
    this.cloudSdkLocation = cloudSdkLocation;
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

  public List<Library> getAppEngineLibraries() {
    return appEngineLibraries;
  }

  public void setAppEngineLibraries(List<Library> libraries) {
    this.appEngineLibraries = new LinkedList<>(libraries);
  }

}
