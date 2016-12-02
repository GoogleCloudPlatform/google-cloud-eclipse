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

package com.google.cloud.tools.eclipse.test.util.project;

import static org.junit.Assert.fail;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.internal.FacetedProjectNature;
import org.junit.rules.ExternalResource;

public final class TestProjectCreator extends ExternalResource {

  private IJavaProject javaProject;
  private String containerPath;
  private List<IProjectFacetVersion> projectFacetVersions = new ArrayList<>();

  public TestProjectCreator withClasspathContainerPath(String containerPath) {
    this.containerPath = containerPath;
    return this;
  }

  public TestProjectCreator withFacetVersions(List<IProjectFacetVersion> projectFacetVersions) {
    this.projectFacetVersions.addAll(projectFacetVersions);
    return this;
  }

  @Override
  protected void before() throws Throwable {
    createJavaProject("test" + Math.random());
  }

  @Override
  protected void after() {
    // Wait for any jobs to complete as WTP validation runs without the workspace protection lock
    ProjectUtils.waitUntilIdle();
    try {
      javaProject.getProject().delete(true, null);
    } catch (CoreException e) {
      fail("Could not delete project");
    }
  }

  public IJavaProject getJavaProject() {
    return javaProject;
  }

  public IProject getProject() {
    return javaProject.getProject();
  }

  private void createJavaProject(String projectName) throws CoreException, JavaModelException {
    IProjectDescription newProjectDescription = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
    newProjectDescription.setNatureIds(new String[]{JavaCore.NATURE_ID, FacetedProjectNature.NATURE_ID});
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    project.create(newProjectDescription, null);
    project.open(null);
    javaProject = JavaCore.create(project);

    addContainerPathToRawClasspath();
    addFacets();
  }

  private void addContainerPathToRawClasspath() throws JavaModelException {
    if (!Strings.isNullOrEmpty(containerPath)) {
      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      IClasspathEntry[] newRawClasspath = new IClasspathEntry[rawClasspath.length + 1];
      System.arraycopy(rawClasspath, 0, newRawClasspath, 0, rawClasspath.length);
      newRawClasspath[newRawClasspath.length - 1] = JavaCore.newContainerEntry(new Path(containerPath));
      javaProject.setRawClasspath(newRawClasspath, null);
    }
  }

  private void addFacets() throws CoreException {
    if (!projectFacetVersions.isEmpty()) {
      IFacetedProject facetedProject = ProjectFacetsManager.create(getProject());
      for (IProjectFacetVersion projectFacetVersion : projectFacetVersions) {
        facetedProject.installProjectFacet(projectFacetVersion, null, null);
      }
    }
  }
}
