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

import static org.junit.Assert.assertNotNull;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.swtbot.SwtBotWorkbenchActions;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.junit.After;
import org.junit.BeforeClass;

/**
 * Common infrastructure for workbench-based tests that create a single project.
 */
public class BaseProjectTest {

  protected static SWTWorkbenchBot bot;
  protected IProject project;

  @BeforeClass
  public static void setUp() throws Exception {
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
      // close editors, so no property changes are dispatched on delete
      bot.closeAllEditors();

      // ensure there are no jobs
      SwtBotWorkbenchActions.waitForProjects(bot, project);
      try {
        SwtBotProjectActions.deleteProject(bot, project.getName());
      } catch (TimeoutException ex) {
        // If this fails it shouldn't fail the test, which has already run
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

  /** Verify that the given type-names (with '.' delimiters) can be resolved for the project. */
  protected static void verifyTypesResolved(IProject project, String... fullyQualifiedTypeNames) {
    IJavaProject javaProject = JavaCore.create(project);
    verifyTypesResolved(javaProject, fullyQualifiedTypeNames);
  }

  /** Verify that the given type-names (with '.' delimiters) can be resolved for the project. */
  protected static void verifyTypesResolved(IJavaProject javaProject,
      String[] fullyQualifiedTypeNames) {
    for (String fullyQualifiedTypeName : fullyQualifiedTypeNames) {
      try {
        IType type = javaProject.findType(fullyQualifiedTypeName, new NullProgressMonitor());
        assertNotNull(String.format("Cannot resolve %s in %s", fullyQualifiedTypeName,
            javaProject.getElementName()), type);
      } catch (JavaModelException ex) {
        throw new AssertionError("Error resolving type: " + fullyQualifiedTypeName, ex);
      }
    }
  }

}
