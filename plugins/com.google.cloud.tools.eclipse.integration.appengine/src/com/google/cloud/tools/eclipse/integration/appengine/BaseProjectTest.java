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

package com.google.cloud.tools.eclipse.integration.appengine;

import com.google.cloud.tools.appengine.operations.CloudSdk;
import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.swtbot.SwtBotWorkbenchActions;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

/** Common infrastructure for workbench-based tests that create a single project. */
@RunWith(SWTBotJunit4ClassRunner.class)
public abstract class BaseProjectTest {
  private static final Logger logger = Logger.getLogger(BaseProjectTest.class.getName());

  protected static SWTWorkbenchBot bot;
  protected IProject project;

  @BeforeClass
  public static void setUp() throws Exception {
    TestUtils.disablePrivacyTos();
    // verify we can find the Google Cloud SDK
    new CloudSdk.Builder().build().validateCloudSdk();

    bot = new SWTWorkbenchBot();
    try {
      SwtBotWorkbenchActions.closeWelcome(bot);
    } catch (WidgetNotFoundException ex) {
      // may receive WNFE: "There is no active view"
    }
  }

  @After
  public void tearDown() {
    if (project != null) {
      // Collapse projects to avoid "No IModelProvider exists for project" errors
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=511541
      SwtBotProjectActions.collapseProjects(bot);

      // close editors, so no property changes are dispatched on delete
      bot.closeAllEditors();
      
      // ensure there are no jobs
      SwtBotWorkbenchActions.waitForProjects(bot, project);

      try {
        project.close(new NullProgressMonitor());
      } catch (CoreException ex) {
        logger.log(Level.SEVERE, "Exception closing test project: " + project, ex);
      }
      try {
        SwtBotProjectActions.deleteProject(bot, project.getName());
      } catch (TimeoutException ex) {
        // If this fails it shouldn't fail the test, which has already run
        logger.log(Level.SEVERE, "Timeout deleting project: " + project.getName(), ex);
        String fileName = SWTBotPreferences.SCREENSHOTS_DIR + "/" + "timeout-" + project.getName()
            + "." + SWTBotPreferences.SCREENSHOT_FORMAT.toLowerCase();
        SWTUtils.captureScreenshot(fileName);
        logger.log(Level.INFO, "Screenshot saved as " + fileName);
      }
      project = null;
    }

    SwtBotWorkbenchActions.resetWorkbench(bot);
  }

  /**
   * Returns the named project; it may not yet exist.
   */
  protected static IProject findProject(String projectName) {
    return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
  }

  /**
   * Return true if a project with the given name exists.
   */
  protected static boolean projectExists(String projectName) {
    IProject project = findProject(projectName);
    return project.exists();
  }
}
