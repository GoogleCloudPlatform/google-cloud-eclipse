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
import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;
import java.io.IOException;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.junit.After;
import org.junit.Test;

/**
 * Tests some cases of installing the App Engine Standard facet on existing projects.
 */
public class StandardFacetInstallationTest {
  private List<IProject> projects;

  @After
  public void tearDown() throws CoreException {
    if (projects != null) {
      for (IProject project : projects) {
        project.delete(true, null);
      }
    }
  }

  @Test
  public void testStandardFacetInstallation() throws IOException, CoreException {
    projects =
        ProjectUtils.importProjects(getClass(), "projects/test-dynamic-web-project.zip", null);
    assertEquals(1, projects.size());
    IProject project = projects.get(0);
    IFacetedProject facetedProject = new FacetedProjectHelper().getFacetedProject(project);
    // verify that the appengine-web.xml is installed in the dynamic web root folder
    AppEngineStandardFacet.installAppEngineFacet(facetedProject, true, null);
    IFile correctAppEngineWebXml = project.getFile(new Path("war/WEB-INF/appengine-web.xml"));
    IFile wrongAppEngineWebXml =
        project.getFile(new Path("src/main/webapp/WEB-INF/appengine-web.xml"));
    assertTrue(correctAppEngineWebXml.exists());
    assertFalse(wrongAppEngineWebXml.exists());

    ProjectUtils.waitUntilIdle();  // App Engine runtime is added via a Job, so wait.
    IRuntime primaryRuntime = facetedProject.getPrimaryRuntime();
    assertTrue(AppEngineStandardFacet.isAppEngineStandardRuntime(primaryRuntime));
  }
}
