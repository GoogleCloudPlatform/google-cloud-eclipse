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

package com.google.cloud.tools.eclipse.appengine.facets;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IConstraint;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineFlexFacetTest {
  @Mock private IFacetedProject facetedProject;

  @Test
  public void testFlexFacetExists() {
    Assert.assertEquals("com.google.cloud.tools.eclipse.appengine.facets.flex",
        AppEngineFlexFacet.ID);
    Assert.assertEquals("1", AppEngineFlexFacet.VERSION);
    Assert.assertTrue(
        ProjectFacetsManager.isProjectFacetDefined(AppEngineFlexFacet.ID));
    Assert.assertEquals(AppEngineFlexFacet.ID, AppEngineFlexFacet.FACET.getId());
    Assert.assertEquals(AppEngineFlexFacet.VERSION,
        AppEngineFlexFacet.FACET_VERSION.getVersionString());
  }

  @Test
  public void testHasAppEngineFacet_withFacet() {
    IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineFlexFacet.ID);
    when(facetedProject.hasProjectFacet(projectFacet)).thenReturn(true);

    Assert.assertTrue(AppEngineFlexFacet.hasFacet(facetedProject));
  }

  @Test
  public void testHasAppEngineFacet_withoutFacet() {
    IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineFlexFacet.ID);
    when(facetedProject.hasProjectFacet(projectFacet)).thenReturn(false);

    Assert.assertFalse(AppEngineFlexFacet.hasFacet(facetedProject));
  }

  @Test
  public void testFacetLabel() {
    IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(AppEngineFlexFacet.ID);
    Assert.assertEquals("App Engine Java Flexible Environment", projectFacet.getLabel());
  }

  @Test
  public void testInstallConstraints_okWithJava7Servlet25() {
    IConstraint constraint = AppEngineFlexFacet.FACET_VERSION.getConstraint();
    IStatus result = constraint.check(Arrays.asList(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25));
    Assert.assertTrue(result.isOK());
  }

  @Test
  public void testInstallConstraints_okWithJava7Servlet30() {
    IConstraint constraint = AppEngineFlexFacet.FACET_VERSION.getConstraint();
    IStatus result = constraint.check(Arrays.asList(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_30));
    Assert.assertTrue(result.isOK());
  }

  @Test
  public void testInstallConstraints_okWithJava7Servlet31() {
    IConstraint constraint = AppEngineFlexFacet.FACET_VERSION.getConstraint();
    IStatus result = constraint.check(Arrays.asList(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_31));
    Assert.assertTrue(result.isOK());
  }

  @Test
  public void testInstallConstraints_okWithJava8Servlet25() {
    IConstraint constraint = AppEngineFlexFacet.FACET_VERSION.getConstraint();
    IStatus result = constraint.check(Arrays.asList(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25));
    Assert.assertTrue(result.isOK());
  }

  @Test
  public void testInstallConstraints_okWithJava8Servlet30() {
    IConstraint constraint = AppEngineFlexFacet.FACET_VERSION.getConstraint();
    IStatus result = constraint.check(Arrays.asList(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_30));
    Assert.assertTrue(result.isOK());
  }

  @Test
  public void testInstallConstraints_okWithJava8Servlet31() {
    IConstraint constraint = AppEngineFlexFacet.FACET_VERSION.getConstraint();
    IStatus result = constraint.check(Arrays.asList(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31));
    Assert.assertTrue(result.isOK());
  }

  @Test
  public void testInstallConstraints_notOkWithNoJava() {
    IConstraint constraint = AppEngineFlexFacet.FACET_VERSION.getConstraint();
    IStatus result = constraint.check(Arrays.asList(WebFacetUtils.WEB_31));
    Assert.assertFalse(result.isOK());
  }

  @Test
  public void testInstallConstraints_notOkWithNoServlet() {
    IConstraint constraint = AppEngineFlexFacet.FACET_VERSION.getConstraint();
    IStatus result = constraint.check(Arrays.asList(JavaFacet.VERSION_1_8));
    Assert.assertFalse(result.isOK());
  }

  @Test
  public void testInstallConstraints_notOkWithServlet24() {
    IConstraint constraint = AppEngineFlexFacet.FACET_VERSION.getConstraint();
    IStatus result = constraint.check(Arrays.asList(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_24));
    Assert.assertFalse(result.isOK());
  }
}
