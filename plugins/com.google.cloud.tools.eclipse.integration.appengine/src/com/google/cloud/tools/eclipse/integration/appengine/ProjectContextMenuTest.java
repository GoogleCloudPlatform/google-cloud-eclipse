/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.integration.appengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IProject;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Rule;
import org.junit.Test;

/** Ensure configured menu options are shown for various types of project. */
public class ProjectContextMenuTest extends BaseProjectTest {
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  @Test
  public void testPlainJavaProject() {
    IProject project = projectCreator.withFacets(JavaFacet.VERSION_1_8).getProject();
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(bot, project.getName());
    assertContextMenuPathNotFound(selected, "Debug As", "2 App Engine");
    assertContextMenuPathNotFound(selected, "Run As", "2 App Engine");
    assertContextMenuPathNotFound(selected, "Deploy to App Engine Standard...");
    assertContextMenuPathFound(selected, "Configure", "Convert to App Engine Standard Project");
    assertContextMenuPathNotFound(
        selected, "Configure", "Reconfigure for App Engine Java 8 runtime");
  }

  @Test
  public void testDynamicWebProjectJava7() {
    IProject project =
        projectCreator.withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25).getProject();
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(bot, project.getName());
    assertContextMenuPathNotFound(selected, "Debug As", "2 App Engine");
    assertContextMenuPathNotFound(selected, "Run As", "2 App Engine");
    assertContextMenuPathNotFound(selected, "Deploy to App Engine Standard...");
    assertContextMenuPathFound(selected, "Configure", "Convert to App Engine Standard Project");
    assertContextMenuPathNotFound(
        selected, "Configure", "Reconfigure for App Engine Java 8 runtime");
  }

  @Test
  public void testDynamicWebProjectJava8() {
    IProject project =
        projectCreator.withFacets(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31).getProject();
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(bot, project.getName());
    assertContextMenuPathNotFound(selected, "Debug As", "2 App Engine");
    assertContextMenuPathNotFound(selected, "Run As", "2 App Engine");
    assertContextMenuPathNotFound(selected, "Deploy to App Engine Standard...");
    assertContextMenuPathFound(selected, "Configure", "Convert to App Engine Standard Project");
    assertContextMenuPathNotFound(
        selected, "Configure", "Reconfigure for App Engine Java 8 runtime");
  }

  @Test
  public void testAppEngineStandardJava7() {
    IProject project =
        projectCreator
            .withFacets(AppEngineStandardFacet.JRE7, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25)
            .getProject();
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(bot, project.getName());
    assertContextMenuPathFound(selected, "Debug As", "2 App Engine");
    assertContextMenuPathFound(selected, "Run As", "2 App Engine");
    assertContextMenuPathFound(selected, "Deploy to App Engine Standard...");
    assertContextMenuPathNotFound(selected, "Configure", "Convert to App Engine Standard Project");
    assertContextMenuPathFound(selected, "Configure", "Reconfigure for App Engine Java 8 runtime");
  }

  @Test
  public void testAppEngineStandardJava8() {
    IProject project =
        projectCreator
            .withFacets(
                AppEngineStandardFacet.FACET.getVersion("JRE8"),
                JavaFacet.VERSION_1_8,
                WebFacetUtils.WEB_31)
            .getProject();
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(bot, project.getName());
    assertContextMenuPathFound(selected, "Debug As", "2 App Engine");
    assertContextMenuPathFound(selected, "Run As", "2 App Engine");
    assertContextMenuPathFound(selected, "Deploy to App Engine Standard...");
    assertContextMenuPathNotFound(selected, "Configure", "Convert to App Engine Standard Project");
    assertContextMenuPathNotFound(
        selected, "Configure", "Reconfigure for App Engine Java 8 runtime");
  }

  /** Verify that the menu items on the path are all present and enabled. */
  private void assertContextMenuPathFound(SWTBotTreeItem projectItem, String... contextMenuPath) {
    assertTrue(contextMenuPath.length > 0);
    SWTBotMenu menu = projectItem.contextMenu(contextMenuPath[0]);
    for (int i = 1; i < contextMenuPath.length; i++) {
      menu = menu.menu(contextMenuPath[i]);
    }
    assertTrue("should be enabled: " + menu.getText(), menu.isEnabled());
  }

  /** Verify that the last item on the path was not present. */
  private void assertContextMenuPathNotFound(
      SWTBotTreeItem projectItem, String... contextMenuPath) {
    assertTrue(contextMenuPath.length > 0);
    if (contextMenuPath.length == 1) {
      assertFalse(projectItem.contextMenu().menuItems().contains(contextMenuPath[0]));
      return;
    }
    SWTBotMenu menu = null;
    try {
      menu = projectItem.contextMenu(contextMenuPath[0]);
      // intermediate menu items should be present
      for (int i = 1; i < contextMenuPath.length - 1; i++) {
        menu = menu.menu(contextMenuPath[i]);
      }
    } catch (WidgetNotFoundException ex) {
      fail("intermediate menus not found");
    }
    assertNotNull(menu);
    assertFalse(menu.menuItems().contains(contextMenuPath[contextMenuPath.length - 1]));
  }
}
