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
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineRuntime;
import com.google.cloud.tools.eclipse.swtbot.MenuMatcher;
import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.resources.IProject;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Ensure configured menu options are shown for various types of project. */
public class ProjectContextMenuTest extends BaseProjectTest {
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();
  @Rule public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  @Test
  public void testPlainJavaProject() {
    IProject project = projectCreator.withFacets(JavaFacet.VERSION_1_8).getProject();
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(bot, project.getName());
    assertThat(
        selected.contextMenu("Debug As"), not(MenuMatcher.hasMenuItem(endsWith("App Engine"))));
    assertThat(
        selected.contextMenu("Run As"), not(MenuMatcher.hasMenuItem(endsWith("App Engine"))));
    assertThat(
        selected.contextMenu(), not(MenuMatcher.hasMenuItem("Deploy to App Engine Standard...")));
    assertThat(
        selected.contextMenu("Configure"),
        MenuMatcher.hasMenuItem("Convert to App Engine Standard Project"));
    assertThat(
        selected.contextMenu("Configure"),
        not(MenuMatcher.hasMenuItem("Reconfigure for App Engine Java 8 runtime")));
  }

  @Test
  public void testDynamicWebProjectJava7() {
    IProject project =
        projectCreator.withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25).getProject();
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(bot, project.getName());
    assertThat(
        selected.contextMenu("Debug As"), not(MenuMatcher.hasMenuItem(endsWith("App Engine"))));
    assertThat(
        selected.contextMenu("Run As"), not(MenuMatcher.hasMenuItem(endsWith("App Engine"))));
    assertThat(
        selected.contextMenu(), not(MenuMatcher.hasMenuItem("Deploy to App Engine Standard...")));
    assertThat(
        selected.contextMenu("Configure"),
        MenuMatcher.hasMenuItem("Convert to App Engine Standard Project"));
    assertThat(
        selected.contextMenu("Configure"),
        not(MenuMatcher.hasMenuItem("Reconfigure for App Engine Java 8 runtime")));
  }

  @Test
  public void testDynamicWebProjectJava8() {
    IProject project =
        projectCreator.withFacets(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_31).getProject();
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(bot, project.getName());
    assertThat(
        selected.contextMenu("Debug As"), not(MenuMatcher.hasMenuItem(endsWith("App Engine"))));
    assertThat(
        selected.contextMenu("Run As"), not(MenuMatcher.hasMenuItem(endsWith("App Engine"))));
    assertThat(
        selected.contextMenu(), not(MenuMatcher.hasMenuItem("Deploy to App Engine Standard...")));
    assertThat(
        selected.contextMenu("Configure"),
        MenuMatcher.hasMenuItem("Convert to App Engine Standard Project"));
    assertThat(
        selected.contextMenu("Configure"),
        not(MenuMatcher.hasMenuItem("Reconfigure for App Engine Java 8 runtime")));
  }

  @Test
  public void testAppEngineStandardJava7() {
    IProject project =
        projectCreator
            .withFacets(AppEngineStandardFacet.JRE7, JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25)
            .getProject();
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(bot, project.getName());
    assertThat(selected.contextMenu("Debug As"), MenuMatcher.hasMenuItem(endsWith("App Engine")));
    assertThat(selected.contextMenu("Run As"), MenuMatcher.hasMenuItem(endsWith("App Engine")));
    assertThat(selected.contextMenu(), MenuMatcher.hasMenuItem("Deploy to App Engine Standard..."));
    assertThat(
        selected.contextMenu("Configure"),
        not(MenuMatcher.hasMenuItem("Convert to App Engine Standard Project")));
    assertThat(
        selected.contextMenu("Configure"),
        MenuMatcher.hasMenuItem("Reconfigure for App Engine Java 8 runtime"));
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
    assertThat(selected.contextMenu("Debug As"), MenuMatcher.hasMenuItem(endsWith("App Engine")));
    assertThat(selected.contextMenu("Run As"), MenuMatcher.hasMenuItem(endsWith("App Engine")));
    assertThat(selected.contextMenu(), MenuMatcher.hasMenuItem("Deploy to App Engine Standard..."));
    assertThat(
        selected.contextMenu("Configure"),
        not(MenuMatcher.hasMenuItem("Convert to App Engine Standard Project")));
    assertThat(
        selected.contextMenu("Configure"),
        not(MenuMatcher.hasMenuItem("Reconfigure for App Engine Java 8 runtime")));
  }

  @Test
  public void testMavenAppEngineStandardJava8() {
    project =
        SwtBotAppEngineActions.createMavenWebAppProject(
            bot,
            "projectContextMenuJava8",
            tempFolder.getRoot().getPath(),
            "com.example.maven7",
            AppEngineRuntime.STANDARD_JAVA_8,
            "com.google.cloud.tools.eclipse.tests",
            "projectContextMenuJava8");
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(bot, project.getName());
    assertThat(selected.contextMenu("Debug As"), MenuMatcher.hasMenuItem(endsWith("App Engine")));
    assertThat(selected.contextMenu("Run As"), MenuMatcher.hasMenuItem(endsWith("App Engine")));
    assertThat(selected.contextMenu(), MenuMatcher.hasMenuItem("Deploy to App Engine Standard..."));
    assertThat(
        selected.contextMenu("Configure"),
        not(MenuMatcher.hasMenuItem("Convert to App Engine Standard Project")));
    assertThat(
        selected.contextMenu("Configure"),
        not(MenuMatcher.hasMenuItem("Reconfigure for App Engine Java 8 runtime")));
  }
}
