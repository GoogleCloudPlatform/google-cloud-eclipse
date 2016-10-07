/*******************************************************************************
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
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.junit.Assert.fail;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.rules.ExternalResource;

import com.google.common.base.Strings;

public final class TestProject extends ExternalResource {

  private IJavaProject javaProject;
  private String containerPath;

  public TestProject() {
    super();
  }

  public TestProject withClasspathContainerPath(String containerPath) {
    this.containerPath = containerPath;
    return this;
  }

  @Override
  protected void before() throws Throwable {
    createJavaProject("test" + Math.random());
  }

  @Override
  protected void after() {
    try {
      javaProject.getProject().delete(true, null);
    } catch (CoreException e) {
      fail("Could not delete project");
    }
  }

  public IJavaProject getJavaProject() {
    return javaProject;
  }

  private void createJavaProject(String projectName) throws CoreException, JavaModelException {
    IProjectDescription newProjectDescription = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
    newProjectDescription.setNatureIds(new String[]{JavaCore.NATURE_ID});
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    project.create(newProjectDescription, null);
    project.open(null);
    javaProject = JavaCore.create(project);
    if (!Strings.isNullOrEmpty(containerPath)) {
      addContainerPathToRawClasspath();
    }
  }

  private void addContainerPathToRawClasspath() throws JavaModelException {
    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    IClasspathEntry[] newRawClasspath = new IClasspathEntry[rawClasspath.length + 1];
    System.arraycopy(rawClasspath, 0, newRawClasspath, 0, rawClasspath.length);
    newRawClasspath[newRawClasspath.length - 1] = JavaCore.newContainerEntry(new Path(containerPath));
    javaProject.setRawClasspath(newRawClasspath, null);
  }
}
