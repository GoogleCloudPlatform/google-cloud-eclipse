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

package com.google.cloud.tools.eclipse.appengine.localserver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.finders.MenuFinder;
import org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

public class RunAppEngineShortcutTest {

  private static final IProjectFacetVersion APP_ENGINE_STANDARD_FACET_1 =
      ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID).getVersion("1");

  @Rule public TestProjectCreator javaProjectCreator = new TestProjectCreator();
  @Rule public TestProjectCreator appEngineProjectCreator = new TestProjectCreator()
      .withFacetVersions(Arrays.asList(
          JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25, APP_ENGINE_STANDARD_FACET_1));

  @Test
  public void testRunAppEngine_enabledForAppEngineProject() {
    IProject project = appEngineProjectCreator.getProject();
    assertTrue(appEngineMenuExists(project));
  }

  @Test
  public void testRunAppEngine_hiddenForPlainProject() {
    IProject project = javaProjectCreator.getProject();
    assertFalse(appEngineMenuExists(project));
  }

  // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1556
  @Test
  public void testRunAppEngine_hiddenEvenIfAppEngineProjectFileIsOpen() throws CoreException {
    // Create an empty file in the App Engine project, and open it in an editor.
    IProject appEngineProject = appEngineProjectCreator.getProject();
    IFile file = appEngineProject.getFile("textfile.txt");
    file.create(new ByteArrayInputStream(new byte[0]), IFile.FORCE, null);

    IWorkbench workbench = PlatformUI.getWorkbench();
    assertNotNull(WorkbenchUtil.openInEditor(workbench, file));

    IProject javaProject = javaProjectCreator.getProject();
    assertFalse(appEngineMenuExists(javaProject));
  }

  // We need regex matching, since the actual menu name is "<number> App Engine".
  private static boolean appEngineMenuExists(IProject project) {
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(
        new SWTWorkbenchBot(), project.getName());
    Menu runAsMenu = selected.contextMenu("Run As").widget.getMenu();
    Matcher<MenuItem> matcher = WidgetMatcherFactory.withRegex("App Engine");

    List<MenuItem> menuItems = new MenuFinder().findMenus(runAsMenu, matcher, false);
    return !menuItems.isEmpty();
  }
}
