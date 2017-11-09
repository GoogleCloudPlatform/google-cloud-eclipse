/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.standard.java8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.convert.AppEngineStandardProjectConvertJob;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.After;
import org.junit.Test;

/**
 * Test end-to-end conversion of existing GPE projects.
 */
public class GpeConversionTest {
  private IProject project;

  @After
  public void tearDown() throws CoreException {
    if (project != null) {
      ProjectUtils.waitForProjects(project);
      project.delete(true, null);
    }
  }

  @Test
  public void gpeClassicProject() throws CoreException, IOException, InterruptedException {
    convertGpeProject(new URL(
        "platform:/fragment/com.google.cloud.tools.eclipse.appengine.compat.test/test-projects/GPE-classic-project.zip"));
  }

  @Test
  public void gpeFacetedProject() throws CoreException, IOException, InterruptedException {
    convertGpeProject(new URL(
        "platform:/fragment/com.google.cloud.tools.eclipse.appengine.compat.test/test-projects/GPE-faceted-project.zip"));
  }

  private void convertGpeProject(URL zipFile)
      throws CoreException, IOException, InterruptedException {
    List<IProject> projects =
        ProjectUtils.importProjects(zipFile,
        false /* checkBuildErrors */, null);
    assertEquals(1, projects.size());
    project = projects.get(0);
    IFacetedProject facetedProject = ProjectFacetsManager.create(project,
        true /* convert to faceted project if necessary */, null /* no monitor here */);

    Job conversionJob = new AppEngineStandardProjectConvertJob(facetedProject);
    conversionJob.schedule();
    conversionJob.join();
    assertTrue("conversion should not have failed", conversionJob.getResult().isOK());

    // ensure facet versions haven't been downgraded
    assertEquals(JavaFacet.VERSION_1_7, facetedProject.getProjectFacetVersion(JavaFacet.FACET));
    assertEquals(WebFacetUtils.WEB_25,
        facetedProject.getProjectFacetVersion(WebFacetUtils.WEB_FACET));
    assertEquals(AppEngineStandardFacet.JRE7,
        facetedProject.getProjectFacetVersion(AppEngineStandardFacet.FACET));
  }

}
