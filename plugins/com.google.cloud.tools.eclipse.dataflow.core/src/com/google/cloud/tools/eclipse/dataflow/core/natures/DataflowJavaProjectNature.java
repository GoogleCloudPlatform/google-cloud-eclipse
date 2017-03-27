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

package com.google.cloud.tools.eclipse.dataflow.core.natures;

import com.google.cloud.tools.eclipse.dataflow.core.DataflowCorePlugin;
import com.google.common.annotations.VisibleForTesting;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import java.util.Arrays;

/**
 * The runtime nature for a Cloud Dataflow Java Project.
 */
public class DataflowJavaProjectNature implements IProjectNature {

  @VisibleForTesting
  static final String DATAFLOW_NATURE_ID = "com.google.cloud.dataflow.DataflowJavaProjectNature";

  private IProject project;

  @Override
  public void configure() throws CoreException {}

  @Override
  public void deconfigure() throws CoreException {}

  @Override
  public IProject getProject() {
    return project;
  }

  @Override
  public void setProject(IProject project) {
    this.project = project;
  }

  /**
   * Adds the Dataflow Nature ID to the {@code IProjectDescription} of the provided project.
   */
  public static void addDataflowJavaNatureToProject(IProject project, IProgressMonitor monitor)
      throws CoreException {
    if (isProjectAccessible(project)) {
      throw new CoreException(new Status(Status.WARNING, DataflowCorePlugin.PLUGIN_ID,
          "Can't add the Dataflow nature to nonexistent or closed project " + project.getName()));
    }
    if (hasDataflowNature(project)) {
      return;
    }
    IProjectDescription description = project.getDescription();
    String[] natureIds = description.getNatureIds();
    String[] newNatureIds = Arrays.copyOf(natureIds, natureIds.length + 1);
    newNatureIds[natureIds.length] = DATAFLOW_NATURE_ID;
    description.setNatureIds(newNatureIds);
    project.setDescription(description, monitor);
  }

  /**
   * Returns true if the project is non-null, accessible (according to {@code
   * IProject#isAccessible()}), and the project description contains the Dataflow Nature ID.
   */
  public static boolean hasDataflowNature(IProject project) throws CoreException {
    if (isProjectAccessible(project)) {
      return false;
    }
    IProjectDescription description = project.getDescription();
    String[] natureIds = description.getNatureIds();
    for (String natureId : natureIds) {
      if (natureId.equals(DATAFLOW_NATURE_ID)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Removes the Dataflow Nature ID from the {@code IProjectDescription} of the provided project.
   */
  public static void removeDataflowJavaNatureFromProject(IProject project, IProgressMonitor monitor)
      throws CoreException {
    if (isProjectAccessible(project)){
      throw new CoreException(new Status(Status.WARNING, DataflowCorePlugin.PLUGIN_ID,
          "Can't remove the Dataflow nature from nonexistent or closed project "
          + project.getName()));
    }
    if (!hasDataflowNature(project)) {
      return;
    }
    IProjectDescription description = project.getDescription();
    String[] natureIds = description.getNatureIds();
    String[] newNatureIds = new String[natureIds.length - 1];
    int newI = 0;
    for (String oldNatureId : natureIds) {
      if (!oldNatureId.equals(DATAFLOW_NATURE_ID)) {
        newNatureIds[newI] = oldNatureId;
        newI++;
      }
    }
    description.setNatureIds(newNatureIds);
    project.setDescription(description, monitor);
  }

  private static boolean isProjectAccessible(IProject project) {
    return project == null || !project.isAccessible();
  }
}
