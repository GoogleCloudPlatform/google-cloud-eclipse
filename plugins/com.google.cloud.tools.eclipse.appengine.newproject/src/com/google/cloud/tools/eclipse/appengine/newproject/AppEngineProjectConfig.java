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

package com.google.cloud.tools.eclipse.appengine.newproject;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.resources.IProject;

/**
 * Collects all data needed to create and configure an App Engine Eclipse Project.
 */
public class AppEngineProjectConfig {
  private File cloudSdkLocation = null;
  private URI eclipseProjectLocationUri = null;
  private String packageName = "";
  private IProject project;
  private List<Library> appEngineLibraries = Collections.emptyList();
  private String serviceName;

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

  public void setAppEngineLibraries(Collection<Library> libraries) {
    this.appEngineLibraries = new ArrayList<>(libraries);
  }

  public String getServiceName() {
    return serviceName;
  }
  
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

}
