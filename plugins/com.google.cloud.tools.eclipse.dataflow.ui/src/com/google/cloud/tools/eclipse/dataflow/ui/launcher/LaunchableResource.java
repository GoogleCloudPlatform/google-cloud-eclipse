/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.launcher;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

/**
 * A resource that can be launched. If the resource is a Java class file that
 * contains a main method, contains a handle to that method and the primary type.
 */
class LaunchableResource {
  private final IResource resource;
  /** The main method of the launchable resource. May be null. */
  private final IMethod mainMethod;
  /** The primary type of the launchable resource. Null if and only if mainMethod is null. */
  private final IType primaryType;

  public LaunchableResource(IResource resource) {
    this(resource, null, null);
  }

  public LaunchableResource(IResource resource, IMethod mainMethod, IType primaryType) {
    this.resource = resource;
    if (mainMethod == null && primaryType != null) {
      throw new NullPointerException("mainMethod is null but primaryType is not null");
    }
    if (mainMethod != null && primaryType == null) {
      throw new NullPointerException("mainMethod is not null but primaryType is null");
    }
    this.mainMethod = mainMethod;
    this.primaryType = primaryType;
  }

  public IMethod getMainMethod() {
    return mainMethod;
  }

  public String getLaunchName() {
    if (mainMethod != null && mainMethod.exists()) {
      return String.format("%s_%s", getProjectName(), primaryType.getElementName());
    } else {
      return getProjectName();
    }   
  }

  public String getProjectName() {
    return resource.getProject().getName();
  }
}
