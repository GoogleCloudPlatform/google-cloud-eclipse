/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.compat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.eclipse.wst.server.core.ServerCore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class GpeMigratorTest {

  @Rule public final TestProjectCreator projectCreator = new TestProjectCreator();

  private IFacetedProject facetedProject;

  @Before
  public void setUp() throws CoreException {
    facetedProject = ProjectFacetsManager.create(projectCreator.getProject());
  }

  @Test
  public void testRemoveObsoleteGpeFixtures_removeGpeGaeNature() throws CoreException {
    // For testing purposes, install GPE nature first.
    IProject project = projectCreator.getProject();
    IProjectDescription description = project.getDescription();
    description.setNatureIds(new String[]{"com.google.appengine.eclipse.core.gaeNature"});
    project.setDescription(description, null);
    assertTrue(project.hasNature("com.google.appengine.eclipse.core.gaeNature"));

    GpeMigrator.removeObsoleteGpeFixtures(facetedProject, null /* monitor */);
    assertFalse(project.hasNature("com.google.appengine.eclipse.core.gaeNature"));
  }

  @Test
  public void testRemoveObsoleteGpeFixtures_removeGpeGaeFacet() throws CoreException {
    IProjectFacetVersion gpeGaeFacet =
        ProjectFacetsManager.getProjectFacet("com.google.appengine.facet").getVersion("1");
    facetedProject.installProjectFacet(gpeGaeFacet, null, null);
    assertTrue(facetedProject.hasProjectFacet(gpeGaeFacet));

    GpeMigrator.removeObsoleteGpeFixtures(facetedProject, null /* monitor */);
    assertFalse(facetedProject.hasProjectFacet(gpeGaeFacet));
  }

  @Test
  public void testRemoveObsoleteGpeFixtures_removeGpeGaeEarFacet() throws CoreException {
    IProjectFacetVersion gpeGaeEarFacet =
        ProjectFacetsManager.getProjectFacet("com.google.appengine.facet.ear").getVersion("1");
    facetedProject.installProjectFacet(gpeGaeEarFacet, null, null);
    assertTrue(facetedProject.hasProjectFacet(gpeGaeEarFacet));

    GpeMigrator.removeObsoleteGpeFixtures(facetedProject, null /* monitor */);
    assertFalse(facetedProject.hasProjectFacet(gpeGaeEarFacet));
  }

  @Test
  public void testRemoveObsoleteGpeFixtures_removeGpeGaeRuntime() throws CoreException {
    // For testing purposes, install GPE runtime first.
    ServerCore.findRuntimeType("com.google.appengine.runtime.id1")
        .createRuntime("com.google.appengine.runtime.id", null).save(true, null);
    IRuntime facetRuntime = FacetUtil.getRuntime(
        ServerCore.findRuntime("com.google.appengine.runtime.id"));
    facetedProject.addTargetedRuntime(facetRuntime, null);
    assertTrue(facetedProject.getTargetedRuntimes().contains(facetRuntime));

    GpeMigrator.removeObsoleteGpeFixtures(facetedProject, null /* monitor */);
    assertFalse(facetedProject.getTargetedRuntimes().contains(facetRuntime));
  }
}
