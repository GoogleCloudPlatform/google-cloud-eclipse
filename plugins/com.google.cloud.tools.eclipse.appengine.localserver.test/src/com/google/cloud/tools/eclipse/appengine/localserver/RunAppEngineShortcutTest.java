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
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.junit.Rule;
import org.junit.Test;

public class RunAppEngineShortcutTest {

  public ThreadDumpingWatchdog watchdog = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  @Rule public TestProjectCreator emptyProjectCreator = new TestProjectCreator();
  @Rule public TestProjectCreator appEngineProjectCreator = new TestProjectCreator()
      .withFacets(JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25, AppEngineStandardFacet.JRE8);

  @Test
  public void testRunAppEngine_enabledForAppEngineProject() {
    IProject project = appEngineProjectCreator.getProject();
    assertTrue(appEngineMenuExists(project));
  }

  @Test
  public void testRunAppEngine_hiddenForPlainProject() {
    IProject project = emptyProjectCreator.getProject();
    assertFalse(appEngineMenuExists(project));
  }

  // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1556
  @Test
  public void testRunAppEngine_hiddenEvenIfAppEngineProjectFileIsOpen() throws CoreException {
    IProject emptyProject = emptyProjectCreator.getProject();

    // Create an empty file in the App Engine project, and open it in an editor.
    IProject appEngineProject = appEngineProjectCreator.getProject();
    IFile file = appEngineProject.getFile("textfile.txt");
    file.create(new ByteArrayInputStream(new byte[0]), IFile.FORCE, null);

    IWorkbench workbench = PlatformUI.getWorkbench();
    assertNotNull(WorkbenchUtil.openInEditor(workbench, file));

    assertFalse(appEngineMenuExists(emptyProject));
  }

  // We need regex matching, since the actual menu name is "<number> App Engine".
  private static boolean appEngineMenuExists(IProject project) {
    SWTBotTreeItem selected = SwtBotProjectActions.selectProject(
        new SWTWorkbenchBot(), project.getName());
    List<String> menuItems = selected.contextMenu("Run As").menuItems();
    Predicate<String> isAppEngineMenu = name -> name.contains("App Engine");
    return menuItems.stream().anyMatch(isAppEngineMenu);
  }
}
