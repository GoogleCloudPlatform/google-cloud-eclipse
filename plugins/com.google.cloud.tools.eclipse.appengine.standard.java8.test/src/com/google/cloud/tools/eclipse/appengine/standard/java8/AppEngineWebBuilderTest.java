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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineConfigurationUtil;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.Collections;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineWebBuilderTest {

  @Rule
  public TestProjectCreator testProject = new TestProjectCreator();

  /** Project without App Engine Standard facet should never have builder. */
  @Test
  public void testNoBuilder() throws CoreException {
    assertProjectMissingBuilder();
  }

  /** Project adding App Engine Standard facet should have our builder. */
  @Test
  public void testAddedBuilder() throws CoreException {
    testProject.withFacets(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8,
        JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25).getFacetedProject();

    assertProjectHasBuilder();
  }

  /** Project removing App Engine Standard facet should not have our builder. */
  @Test
  public void testBuilderRemoved() throws CoreException {
    testProject.withFacets(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8,
        JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25).getFacetedProject();
    assertProjectHasBuilder();

    Action uninstallAction =
        new Action(Action.Type.UNINSTALL,
            AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8, null);
    testProject.getFacetedProject().modify(Collections.singleton(uninstallAction), null);
    assertProjectMissingBuilder();
  }

  /**
   * Adding <runtime>java8</runtime> to appengine-web.xml should upgrade the Java and Dynamic Web
   * Project facets to 1.8 and 3.1 respectively.
   */
  @Test
  public void testAddJava8Runtime() throws CoreException {
    testProject
        .withFacets(AppEngineStandardFacet.JRE7, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25)
        .getFacetedProject();
    assertProjectHasBuilder();

    IFile appEngineWebDescriptor =
        AppEngineConfigurationUtil.findConfigurationFile(
            testProject.getProject(), new Path("appengine-web.xml"));
    assertTrue("should have appengine-web.xml",
        appEngineWebDescriptor != null && appEngineWebDescriptor.exists());

    AppEngineDescriptorTransform.addJava8Runtime(appEngineWebDescriptor);
    ProjectUtils.waitForProjects(testProject.getProject());

    // adding <runtime>java8</runtime> should change java to 1.8 and dwp to 3.1
    assertFacetVersions(testProject.getFacetedProject(),
        AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8, JavaFacet.VERSION_1_8,
        WebFacetUtils.WEB_31);
  }

  /**
   * Removing <runtime>java8</runtime> from appengine-web.xml should always downgrade the Java and
   * Dynamic Web Project facets to 1.7 and 2.5 respectively.
   */
  @Test
  public void testRemovingJava8Runtime() throws CoreException {
    testProject.withFacets(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8,
        JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31).getFacetedProject();
    assertProjectHasBuilder();

    IFile appEngineWebDescriptor =
        AppEngineConfigurationUtil.findConfigurationFile(
            testProject.getProject(), new Path("appengine-web.xml"));
    assertTrue("should have appengine-web.xml",
        appEngineWebDescriptor != null && appEngineWebDescriptor.exists());

    AppEngineDescriptorTransform.removeJava8Runtime(appEngineWebDescriptor);
    ProjectUtils.waitForProjects(testProject.getProject());
    assertFacetVersions(testProject.getFacetedProject(), AppEngineStandardFacet.JRE7,
        JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25);
  }

  /**
   * Removing <runtime>java8</runtime> from appengine-web.xml should always downgrade the Java and
   * Dynamic Web Project facets to 1.7 and 2.5 respectively.
   */
  @Test
  public void testRemovingJava8Runtime_webFacet() throws CoreException {
    testProject.withFacets(AppEngineStandardFacetChangeListener.APP_ENGINE_STANDARD_JRE8,
        JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31).getFacetedProject();
    assertProjectHasBuilder();

    IFile appEngineWebDescriptor =
        AppEngineConfigurationUtil.findConfigurationFile(
            testProject.getProject(), new Path("appengine-web.xml"));
    assertTrue("should have appengine-web.xml",
        appEngineWebDescriptor != null && appEngineWebDescriptor.exists());

    AppEngineDescriptorTransform.removeJava8Runtime(appEngineWebDescriptor);
    ProjectUtils.waitForProjects(testProject.getProject());
    // removing <runtime>java8</runtime> should change java to 1.7
    // removing <runtime>java8</runtime> should change jst.web to 2.5
    assertFacetVersions(testProject.getFacetedProject(), AppEngineStandardFacet.JRE7,
        JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25);
  }

  private void assertProjectMissingBuilder() throws CoreException {
    ProjectUtils.waitForProjects(testProject.getProject());
    IProjectDescription description = testProject.getProject().getDescription();
    for (ICommand buildSpec : description.getBuildSpec()) {
      assertNotEquals(AppEngineWebBuilder.BUILDER_ID, buildSpec.getBuilderName());
    }
  }

  private void assertProjectHasBuilder() throws CoreException {
    ProjectUtils.waitForProjects(testProject.getProject());
    IProjectDescription description = testProject.getProject().getDescription();
    for (ICommand buildSpec : description.getBuildSpec()) {
      if (AppEngineWebBuilder.BUILDER_ID.equals(buildSpec.getBuilderName())) {
        return;
      }
    }
    fail("missing AppEngineWebBuilder");
  }

  private void assertFacetVersions(IFacetedProject project, IProjectFacetVersion... versions) {
    for (IProjectFacetVersion version : versions) {
      assertEquals(version, project.getProjectFacetVersion(version.getProjectFacet()));
    }
  }

}
