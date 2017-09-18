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

package com.google.cloud.tools.eclipse.appengine.deploy.ui.flexible;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexJarFacet;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexWarFacet;
import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import com.google.cloud.tools.eclipse.util.NatureUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Rule;
import org.junit.Test;

public class FlexDeployCommandHandlerTest {

  @Rule public TestProjectCreator javaProjectCreator = new TestProjectCreator()
      .withFacetVersions(JavaFacet.VERSION_1_8);

  @Rule public TestProjectCreator flexJarProjectCreator = new TestProjectCreator()
      .withFacetVersions(JavaFacet.VERSION_1_8, AppEngineFlexJarFacet.FACET_VERSION);

  @Rule public TestProjectCreator flexWarProjectCreator = new TestProjectCreator()
      .withFacetVersions(
          JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31, AppEngineFlexWarFacet.FACET_VERSION);

  @Test
  public void testGetStagingDelegate_exceptionIfAppYamlDoesNotExist() {
    try {
      FlexDeployCommandHandler handler = new FlexDeployCommandHandler();
      handler.getStagingDelegate(javaProjectCreator.getProject());
      fail();
    } catch (CoreException e) {
      // The path separator is always '/' (IPath.SEPARATOR) regardless of platforms.
      assertThat(e.getMessage(), endsWith("/src/main/appengine/app.yaml does not exist."));
    }
  }

  @Test
  public void testDeployContextMenu_hiddenForNonFlexProject() {
    IProject project = javaProjectCreator.getProject();
    ProjectUtils.waitForProjects(project);
    assertFalse(flexDeployMenuVisible(project));
  }

  @Test
  public void testDeployContextMenu_visibleForFlexWarProject() {
    IProject project = flexWarProjectCreator.getProject();
    ProjectUtils.waitForProjects(project);
    assertTrue(flexDeployMenuVisible(project));
  }

  @Test
  public void testDeployContextMenu_visibleForFlexJarMavenProject() throws CoreException {
    IProject project = flexJarProjectCreator.getProject();
    NatureUtils.addNature(project, MavenUtils.MAVEN2_NATURE_ID, null);
    ProjectUtils.waitForProjects(project);
    assertTrue(flexDeployMenuVisible(project));
  }

  @Test
  public void testDeployContextMenu_hiddenForFlexJarNonMavenProject() {
    IProject project = flexJarProjectCreator.getProject();
    ProjectUtils.waitForProjects(project);
    assertFalse(flexDeployMenuVisible(project));
  }

  private static boolean flexDeployMenuVisible(IProject project) {
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(
        new SWTWorkbenchBot(), project.getName());
    try {
      selected.contextMenu("Deploy to App Engine Flexible...");
      return true;
    } catch (WidgetNotFoundException e) {
      return false;
    }
  }
}
