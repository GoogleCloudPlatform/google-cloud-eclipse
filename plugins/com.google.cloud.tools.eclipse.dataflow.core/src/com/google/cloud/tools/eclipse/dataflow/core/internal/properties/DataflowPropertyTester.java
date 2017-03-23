/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.internal.properties;

import com.google.cloud.tools.eclipse.dataflow.core.DataflowCorePlugin;
import com.google.cloud.tools.eclipse.dataflow.core.natures.DataflowJavaProjectNature;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

/**
 * A PropertyTester for Dataflow Project properties.
 */
public class DataflowPropertyTester extends PropertyTester {

  private static final String IS_IN_DATAFLOW_PROJECT_PROPERTY = "isInDataflowProject";
  private static final String IS_DATAFLOW_PROJECT_PROPERTY = "isDataflowProject";
  private static final String PROJECT_IS_NOT_DATAFLOW_PROJECT_PROPERTY =
      "projectIsNotDataflowProject";

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    switch (property) {
      case IS_IN_DATAFLOW_PROJECT_PROPERTY:
        return testIsInDataflowProject(receiver);
      case IS_DATAFLOW_PROJECT_PROPERTY:
        return testIsDataflowProject(receiver);
      case PROJECT_IS_NOT_DATAFLOW_PROJECT_PROPERTY:
        return testProjectIsNotDataflowProject(receiver);
      default:
        return false;
    }
  }

  private boolean testProjectIsNotDataflowProject(Object receiver) {
    if (receiver instanceof IAdaptable) {
      IAdaptable adaptable = (IAdaptable) receiver;
      IProject project = (IProject) adaptable.getAdapter(IProject.class);
      return !isProjectDataflowProject(project);
    }
    return false;
  }

  private boolean testIsDataflowProject(Object receiver) {
    if (receiver instanceof IAdaptable) {
      IAdaptable adaptable = (IAdaptable) receiver;
      IProject project = (IProject) adaptable.getAdapter(IProject.class);
      return isProjectDataflowProject(project);
    }
    return false;
  }

  private boolean testIsInDataflowProject(Object receiver) {
    if (receiver instanceof IAdaptable) {
      IAdaptable adaptable = (IAdaptable) receiver;
      IResource resource = (IResource) adaptable.getAdapter(IResource.class);
      if (resource != null) {
        IProject project = resource.getProject();
        return isProjectDataflowProject(project);
      }
    }
    return false;
  }

  private boolean isProjectDataflowProject(IProject project) {
    try {
      if (project != null && DataflowJavaProjectNature.hasDataflowNature(project)) {
        return true;
      }
    } catch (CoreException e) {
      DataflowCorePlugin.logError(e, "Exception while testing for Dataflow Nature");
    }
    return false;
  }
}
