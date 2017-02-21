/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.projectselector.model;

/**
 * Represents a Google Cloud Platform project.
 */
public class GcpProject {

  private final String name;
  private final String id;
  private AppEngine appEngine;

  public GcpProject(String name, String id) {
    this.name = name;
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  // Simplified from the Eclipse generated hashcode()
  @Override
  public int hashCode() {
    return id == null ? 0 : id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    GcpProject other = (GcpProject) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  /**
   * @return an AppEngine object if it has been retrieved from the backend or {@code null} if it has
   * not yet been retrieved
   */
  public AppEngine getAppEngine() {
    return appEngine;
  }

  public void setAppEngine(AppEngine appEngine) {
    this.appEngine = appEngine;
  }

  /**
   * @return {@code true} if the AppEngine object has been created based on the information
   * retrieved from the backend
   */
  public boolean hasAppEngineInfo() {
    return appEngine != null;
  }
}
