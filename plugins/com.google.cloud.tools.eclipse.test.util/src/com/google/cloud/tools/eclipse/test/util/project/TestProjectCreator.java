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

import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.facets.FacetUtil;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.internal.FacetedProjectNature;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerUtil;
import org.junit.rules.ExternalResource;

/**
 * Utility class to create and configure a Faceted Project. Installs a Java 1.7 facet if no facets
 * are specified with {@link #withFacetVersions}.
 */
@SuppressWarnings("restriction") // For FacetedProjectNature
public final class TestProjectCreator extends ExternalResource {

  private IProject project;
  private IJavaProject javaProject;
  private String containerPath;
  private String appEngineServiceId;
  private List<IProjectFacetVersion> projectFacetVersions = new ArrayList<>();

  public TestProjectCreator withClasspathContainerPath(String containerPath) {
    this.containerPath = containerPath;
    return this;
  }

  public TestProjectCreator withFacetVersions(IProjectFacetVersion... projectFacetVersions) {
    Collections.addAll(this.projectFacetVersions, projectFacetVersions);
    return this;
  }

  public TestProjectCreator withFacetVersions(List<IProjectFacetVersion> projectFacetVersions) {
    this.projectFacetVersions.addAll(projectFacetVersions);
    return this;
  }

  public TestProjectCreator withAppEngineServiceId(String serviceId) {
    appEngineServiceId = serviceId;
    return this;
  }

  @Override
  protected void after() {
    if (project != null) {
      // Wait for any jobs to complete as WTP validation runs without the workspace protection lock
      ProjectUtils.waitForProjects(project);
      try {
        project.delete(true, null);
      } catch (CoreException ex) {
        throw new AssertionError("Could not delete project", ex);
      }
    }
  }

  public IModule getModule() {
    createProjectIfNecessary();
    return ServerUtil.getModule(project);
  }

  public IJavaProject getJavaProject() {
    createProjectIfNecessary();
    return javaProject;
  }

  public IProject getProject() {
    createProjectIfNecessary();
    return project;
  }

  public IFacetedProject getFacetedProject() throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(getProject());
    return facetedProject;
  }


  private void createProjectIfNecessary() {
    if (project == null) {
      try {
        createProject("test" + Math.random());
      } catch (CoreException ex) {
        throw new AssertionError("Could not create project", ex);
      }
    }
  }

  private void createProject(String projectName) throws CoreException {
    IProjectDescription newProjectDescription =
        ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
    newProjectDescription.setNatureIds(
        new String[] {FacetedProjectNature.NATURE_ID});
    project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    project.create(newProjectDescription, null);
    project.open(null);

    addFacets();
    addContainerPathToRawClasspath();
    if (appEngineServiceId != null) {
      setAppEngineServiceId(appEngineServiceId);
    }
  }

  private void addContainerPathToRawClasspath() throws JavaModelException {
    if (!Strings.isNullOrEmpty(containerPath)) {
      Preconditions.checkNotNull(javaProject);
      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      IClasspathEntry[] newRawClasspath = new IClasspathEntry[rawClasspath.length + 1];
      System.arraycopy(rawClasspath, 0, newRawClasspath, 0, rawClasspath.length);
      newRawClasspath[newRawClasspath.length - 1] =
          JavaCore.newContainerEntry(new Path(containerPath));
      javaProject.setRawClasspath(newRawClasspath, null);
    }
  }

  private void addFacets() throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(getProject());

    FacetUtil facetUtil = new FacetUtil(facetedProject);
    for (IProjectFacetVersion projectFacetVersion : projectFacetVersions) {
      facetUtil.addFacetToBatch(projectFacetVersion, null);
    }
    facetUtil.install(null);

    // App Engine runtime is added via a Job, so wait.
    ProjectUtils.waitForProjects(getProject());

    if (facetedProject.hasProjectFacet(JavaFacet.FACET)) {
      javaProject = JavaCore.create(project);
      assertTrue(javaProject.exists());
    }
  }

  public void setAppEngineServiceId(String serviceId) throws CoreException {
    IFolder webinf = WebProjectUtil.getWebInfDirectory(getProject());
    IFile descriptorFile = webinf.getFile("appengine-web.xml");
    assertTrue("Project should have AppEngine Standard facet", descriptorFile.exists());
    StringBuilder newAppEngineWebDescriptor = new StringBuilder();
    newAppEngineWebDescriptor
        .append("<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>\n");
    newAppEngineWebDescriptor.append("<service>").append(serviceId).append("</service>\n");
    newAppEngineWebDescriptor.append("</appengine-web-app>\n");
    InputStream contents = new ByteArrayInputStream(
        newAppEngineWebDescriptor.toString().getBytes(StandardCharsets.UTF_8));
    descriptorFile.setContents(contents, IFile.FORCE, null);
  }

}
