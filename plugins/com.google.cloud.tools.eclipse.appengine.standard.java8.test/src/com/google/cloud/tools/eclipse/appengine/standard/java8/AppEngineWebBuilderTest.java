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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
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
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineWebBuilderTest {

  @Rule
  public TestProjectCreator testProject = new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25);

  /** Project without App Engine Standard facet should never have builder. */
  @Test
  public void testNoBuilder() throws CoreException {
    assertProjectMissingBuilder();
  }

  /** Project adding App Engine Standard facet should have builder. */
  @Test
  public void testAddedBuilder() throws CoreException {
    Action installAction =
        new Action(Action.Type.INSTALL, AppEngineStandardFacet.FACET_VERSION, null);
    testProject.getFacetedProject().modify(Collections.singleton(installAction), null);

    assertProjectHasBuilder();
  }

  /** Project adding App Engine Standard facet should have builder. */
  @Test
  public void testRemovedBuilder() throws CoreException {
    Action installAction =
        new Action(Action.Type.INSTALL, AppEngineStandardFacet.FACET_VERSION, null);
    testProject.getFacetedProject().modify(Collections.singleton(installAction), null);
    assertProjectHasBuilder();

    Action uninstallAction =
        new Action(Action.Type.UNINSTALL, AppEngineStandardFacet.FACET_VERSION, null);
    testProject.getFacetedProject().modify(Collections.singleton(uninstallAction), null);
    assertProjectMissingBuilder();
  }

  /** Project adding App Engine Standard facet should have builder. */
  @Test
  public void testAddingJava8Runtime() throws CoreException {
    Action installAction =
        new Action(Action.Type.INSTALL, AppEngineStandardFacet.FACET_VERSION, null);
    testProject.getFacetedProject().modify(Collections.singleton(installAction), null);
    assertProjectHasBuilder();

    IFile appEngineWebDescriptor = WebProjectUtil.findInWebInf(testProject.getProject(),
        new Path("appengine-web.xml"));
    assertTrue("missing appengine-web.xml",
        appEngineWebDescriptor != null && appEngineWebDescriptor.exists());

    assertTrue(testProject.getFacetedProject().hasProjectFacet(JavaFacet.VERSION_1_7));
    assertTrue(testProject.getFacetedProject().hasProjectFacet(WebFacetUtils.WEB_25));

    AppEngineDescriptorTransform.addJava8Runtime(appEngineWebDescriptor);
    ProjectUtils.waitForProjects(testProject.getProject());
    assertTrue(testProject.getFacetedProject().hasProjectFacet(JavaFacet.VERSION_1_8));
    assertTrue(testProject.getFacetedProject().hasProjectFacet(WebFacetUtils.WEB_25));

    AppEngineDescriptorTransform.removeJava8Runtime(appEngineWebDescriptor);
    ProjectUtils.waitForProjects(testProject.getProject());
    assertTrue(testProject.getFacetedProject().hasProjectFacet(JavaFacet.VERSION_1_7));
    assertTrue(testProject.getFacetedProject().hasProjectFacet(WebFacetUtils.WEB_25));
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
}
