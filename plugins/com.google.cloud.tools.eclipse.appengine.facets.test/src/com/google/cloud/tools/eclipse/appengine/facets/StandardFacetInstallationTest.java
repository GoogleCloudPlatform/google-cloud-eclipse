/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.facets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests some cases of installing the App Engine Standard facet on existing projects.
 */
public class StandardFacetInstallationTest {
  private List<IProject> projects;

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  @After
  public void tearDown() throws CoreException {
    if (projects != null) {
      for (IProject project : projects) {
        try {
          project.delete(true, null);
        } catch (IllegalArgumentException ex) {
          // Get more information to diagnose odd test failures; remove when fixed
          // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1196
          System.err.println("JobManager state:\n" + Job.getJobManager());
          System.err.println("  Current job: " + Job.getJobManager().currentJob());
          System.err.println("  Current rule: " + Job.getJobManager().currentRule());
        }
      }
    }
  }

  @Test
  public void testStandardFacetInstallation() throws IOException, CoreException {
    projects = ProjectUtils.importProjects(getClass(),
        "projects/test-dynamic-web-project.zip", true /* checkBuildErrors */, null);
    assertEquals(1, projects.size());
    IProject project = projects.get(0);
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    // verify that the appengine-web.xml is installed in the dynamic web root folder
    AppEngineStandardFacet.installAppEngineFacet(facetedProject, true, null);
    IFile correctAppEngineWebXml = project.getFile("war/WEB-INF/appengine-web.xml");
    IFile wrongAppEngineWebXml = project.getFile("src/main/webapp/WEB-INF/appengine-web.xml");
    assertTrue(correctAppEngineWebXml.exists());
    assertFalse(wrongAppEngineWebXml.exists());

    ProjectUtils.waitForProjects(project); // App Engine runtime is added via a Job, so wait.
    IRuntime primaryRuntime = facetedProject.getPrimaryRuntime();
    assertTrue(AppEngineStandardFacet.isAppEngineStandardRuntime(primaryRuntime));
  }

  @Test
  public void testStandardFacetInstallation_createsWebXml() throws CoreException {
    IProject project = projectCreator.getProject();
    assertFalse(project.getFile("src/main/webapp/WEB-INF/web.xml").exists());

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    AppEngineStandardFacet.installAppEngineFacet(facetedProject, true, null);
    ProjectUtils.waitForProjects(project); // App Engine runtime is added via a Job, so wait.

    assertTrue(project.getFile("src/main/webapp/WEB-INF/web.xml").exists());
  }

  @Test
  public void testStandardFacetInstallation_doesNotOverwriteWebXml()
      throws CoreException, IOException {
    // Create an empty web.xml.
    IProject project = projectCreator.getProject();
    createFolders(project, new Path("src/main/webapp/WEB-INF"));
    IFile webXml = project.getFile("src/main/webapp/WEB-INF/web.xml");
    webXml.create(new ByteArrayInputStream(new byte[0]), true, null);
    assertEmptyFile(webXml);

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    AppEngineStandardFacet.installAppEngineFacet(facetedProject, true, null);
    ProjectUtils.waitForProjects(project); // App Engine runtime is added via a Job, so wait.

    assertEmptyFile(webXml);
  }

  private static void createFolders(IContainer parent, IPath path) throws CoreException {
    if (!path.isEmpty()) {
      IFolder folder = parent.getFolder(new Path(path.segment(0)));
      if (!folder.exists()) {
        folder.create(true, true, null);
      }
      createFolders(folder, path.removeFirstSegments(1));
    }
  }

  private static void assertEmptyFile(IFile file) throws IOException, CoreException {
    try (InputStream in = file.getContents()) {
      assertEquals(-1, in.read());  // Verify it is an empty file.
    }
  }
}
