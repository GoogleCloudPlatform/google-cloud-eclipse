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
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.wst.common.componentcore.internal.builder.DependencyGraphImpl;
import org.eclipse.wst.common.componentcore.internal.builder.IDependencyGraph;
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
  protected void before() throws Throwable {
    createProject("test" + Math.random());
  }

  @Override
  protected void after() {
    // Wait for any jobs to complete as WTP validation runs without the workspace protection lock
    ProjectUtils.waitForProjects(project);
    try {
      project.delete(true, null);
    } catch (CoreException e) {
      fail("Could not delete project");
    }
  }

  public IModule getModule() {
    return ServerUtil.getModule(project);
  }

  public IJavaProject getJavaProject() {
    return javaProject;
  }

  public IProject getProject() {
    return project;
  }

  private void createProject(String projectName) throws CoreException, JavaModelException {
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

    Set<IFacetedProject.Action> facetInstallSet = new HashSet<>();
    for (IProjectFacetVersion projectFacetVersion : projectFacetVersions) {
      facetInstallSet.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL,
          projectFacetVersion, null));
    }

    // Workaround deadlock bug described in Eclipse bug (https://bugs.eclipse.org/511793).
    // There are graph update jobs triggered by the completion of the CreateProjectOperation
    // above (from resource notifications) and from other resource changes from modifying the
    // project facets. So we force the dependency graph to defer updates.
    try {
      IDependencyGraph.INSTANCE.preUpdate();
      try {
        Job.getJobManager().join(DependencyGraphImpl.GRAPH_UPDATE_JOB_FAMILY, null);
      } catch (OperationCanceledException | InterruptedException ex) {
        throw new RuntimeException("Exception waiting for DependencyGraph job", ex);
      }

      facetedProject.modify(facetInstallSet, null);
    } finally {
      IDependencyGraph.INSTANCE.postUpdate();
    }

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
