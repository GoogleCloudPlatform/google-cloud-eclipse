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

package com.google.cloud.tools.eclipse.appengine.facets;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.Arrays;
import java.util.Collections;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IConstraint;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineFlexJarFacetTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator().withFacets();
  @Rule public TestProjectCreator javaProjectCreator = new TestProjectCreator()
      .withFacets(JavaFacet.VERSION_1_7);

  @Mock private IFacetedProject mockFacetedProject;

  @Test
  public void testFlexFacetExists() {
    Assert.assertEquals("com.google.cloud.tools.eclipse.appengine.facets.flex.jar",
        AppEngineFlexJarFacet.ID);
    Assert.assertEquals("1", AppEngineFlexJarFacet.VERSION);
    Assert.assertTrue(ProjectFacetsManager.isProjectFacetDefined(AppEngineFlexJarFacet.ID));
    Assert.assertEquals(AppEngineFlexJarFacet.ID, AppEngineFlexJarFacet.FACET.getId());
    Assert.assertEquals(AppEngineFlexJarFacet.VERSION,
        AppEngineFlexJarFacet.FACET_VERSION.getVersionString());
  }

  @Test
  public void testHasAppEngineFacet_withFacet() {
    IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineFlexJarFacet.ID);
    when(mockFacetedProject.hasProjectFacet(projectFacet)).thenReturn(true);

    Assert.assertTrue(AppEngineFlexJarFacet.hasFacet(mockFacetedProject));
  }

  @Test
  public void testHasAppEngineFacet_withoutFacet() {
    IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineFlexJarFacet.ID);
    when(mockFacetedProject.hasProjectFacet(projectFacet)).thenReturn(false);

    Assert.assertFalse(AppEngineFlexJarFacet.hasFacet(mockFacetedProject));
  }

  @Test
  public void testFacetLabel() {
    IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineFlexJarFacet.ID);
    Assert.assertEquals("App Engine Java Flexible Environment (JAR)", projectFacet.getLabel());
  }

  @Test
  public void testInstallConstraints_okWithJava7() {
    IConstraint constraint = AppEngineFlexJarFacet.FACET_VERSION.getConstraint();
    IStatus result = constraint.check(Arrays.asList(JavaFacet.VERSION_1_7));
    Assert.assertTrue(result.isOK());
  }

  @Test
  public void testInstallConstraints_okWithJava8() {
    IConstraint constraint = AppEngineFlexJarFacet.FACET_VERSION.getConstraint();
    IStatus result = constraint.check(Arrays.asList(JavaFacet.VERSION_1_8));
    Assert.assertTrue(result.isOK());
  }

  @Test
  public void testInstallConstraints_notOkWithNoJava() {
    IConstraint constraint = AppEngineFlexJarFacet.FACET_VERSION.getConstraint();
    IStatus result = constraint.check(Collections.<IProjectFacetVersion>emptyList());
    Assert.assertFalse(result.isOK());
  }

  @Test
  public void testInstallConstraints_notOkWithServlet() {
    IConstraint constraint = AppEngineFlexJarFacet.FACET_VERSION.getConstraint();
    IStatus result = constraint.check(Arrays.asList(WebFacetUtils.WEB_31));
    Assert.assertFalse(result.isOK());
  }

  @Test
  public void testInstallAppEngineFacet_installDependentFacets() throws CoreException {
    IProject project = projectCreator.getProject();
    IFacetedProject facetedProject = projectCreator.getFacetedProject();

    AppEngineFlexJarFacet.installAppEngineFacet(facetedProject,
        true /* installDependentFacets */, new NullProgressMonitor());
    Assert.assertTrue(AppEngineFlexJarFacet.hasFacet(facetedProject));
    Assert.assertTrue(facetedProject.hasProjectFacet(JavaFacet.VERSION_1_8));
    Assert.assertTrue(project.getFile("src/main/appengine/app.yaml").exists());
  }

  @Test
  public void testInstallAppEngineFacet_noDependentFacets() {
    IFacetedProject facetedProject = projectCreator.getFacetedProject();
    try {
      AppEngineFlexJarFacet.installAppEngineFacet(facetedProject,
          false /* installDependentFacets */, new NullProgressMonitor());
      fail();
    } catch (CoreException e) {
      Assert.assertNotNull(e.getMessage());
    }
  }

  @Test
  public void testInstallAppEngineFacet_onJavaProject() throws CoreException {
    IProject project = javaProjectCreator.getProject();
    IFacetedProject facetedProject = javaProjectCreator.getFacetedProject();

    AppEngineFlexJarFacet.installAppEngineFacet(facetedProject,
        false /* installDependentFacets */, new NullProgressMonitor());
    Assert.assertTrue(AppEngineFlexJarFacet.hasFacet(facetedProject));
    Assert.assertTrue(facetedProject.hasProjectFacet(JavaFacet.VERSION_1_7));
    Assert.assertTrue(project.getFile("src/main/appengine/app.yaml").exists());
  }
}
