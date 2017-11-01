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

  public enum Template { DEFAULT, SPRING_BOOT };

  private File cloudSdkLocation = null;
  private URI eclipseProjectLocationUri = null;
  private String packageName = "";
  private IProject project;
  private List<Library> appEngineLibraries = Collections.emptyList();
  private String serviceName;
  private String runtimeId;
  private Template template = Template.DEFAULT;

  private boolean useMaven;
  private String mavenGroupId;
  private String mavenArtifactId;
  private String mavenVersion;

  File getCloudSdkLocation() {
    return cloudSdkLocation;
  }

  void setCloudSdkLocation(File cloudSdkLocation) {
    this.cloudSdkLocation = cloudSdkLocation;
  }

  void setPackageName(String name) {
    packageName = name;
  }

  String getPackageName() {
    return packageName;
  }

  /**
   * Null project location URI means the default location.
   */
  void setProject(IProject project) {
    this.project = project;
  }

  IProject getProject() {
    return project;
  }

  URI getEclipseProjectLocationUri() {
    return eclipseProjectLocationUri;
  }

  void setEclipseProjectLocationUri(URI uri) {
    eclipseProjectLocationUri = uri;
  }

  List<Library> getAppEngineLibraries() {
    return appEngineLibraries;
  }

  public void setAppEngineLibraries(Collection<Library> libraries) {
    appEngineLibraries = new ArrayList<>(libraries);
  }

  String getServiceName() {
    return serviceName;
  }

  void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  String getRuntimeId() {
    return runtimeId;
  }

  void setRuntimeId(String runtimeId) {
    this.runtimeId = runtimeId;
  }

  Template getTemplate() {
    return template;
  }

  void setTemplate(Template template) {
    this.template = template;
  }

  void setUseMaven(String mavenGroupId, String mavenArtifactId, String mavenVersion) {
    useMaven = true;
    this.mavenGroupId = mavenGroupId;
    this.mavenArtifactId = mavenArtifactId;
    this.mavenVersion = mavenVersion;
  }

  public boolean getUseMaven() {
    return useMaven;
  }

  String getMavenGroupId() {
    return mavenGroupId;
  }

  String getMavenArtifactId() {
    return mavenArtifactId;
  }

  String getMavenVersion() {
    return mavenVersion;
  }
}
