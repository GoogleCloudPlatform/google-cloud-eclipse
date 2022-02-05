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

import com.google.cloud.tools.eclipse.appengine.ui.AppEngineRuntime;
import com.google.cloud.tools.eclipse.swtbot.SwtBotTestingUtilities;
import com.google.cloud.tools.eclipse.swtbot.SwtBotTimeoutManager;
import com.google.cloud.tools.eclipse.swtbot.SwtBotTreeUtilities;
import com.google.cloud.tools.eclipse.swtbot.SwtBotWorkbenchActions;
import java.io.File;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;
import org.junit.Assert;

/**
 * Useful App Engine-related actions for Google Cloud Tools for Eclipse.
 */
public class SwtBotAppEngineActions {

  /**
   * Create a native App Engine project using the
   * {@code com.google.cloud.tools.eclipse.appengine.newproject.AppEngineStandard} new wizard.
   *
   * @param bot the bot
   * @param projectName the name of the project
   * @param location can be {@code null}
   * @param javaPackage can be {@code null} or empty
   * @return the project
   */
  public static IProject createNativeWebAppProject(SWTWorkbenchBot bot, String projectName,
      String location, String javaPackage, AppEngineRuntime runtime) {
    return createWebAppProject(bot, projectName, location, javaPackage, runtime,
        null /* extraBotActions */);
  }

  /**
   * Create a new project with the Maven-based Google App Engine Standard Java Project wizard
   */
  public static IProject createMavenWebAppProject(SWTWorkbenchBot bot, String projectName,
      String location, String javaPackage, AppEngineRuntime runtime, String mavenGroupId,
      String mavenArtifactId) {
    return createWebAppProject(bot, projectName, location, javaPackage, runtime, () -> {
      bot.checkBox("Create as Maven project").click();
      bot.textWithLabel("Group ID:").setText(mavenGroupId);
      bot.textWithLabel("Artifact ID:").setText(mavenArtifactId);
    });
  }

  public static IProject createWebAppProject(SWTWorkbenchBot bot, String projectName,
      String location, String javaPackage, AppEngineRuntime runtime, Runnable extraBotActions) {
    bot.menu("File").menu("New").menu("Project...").click();

    SWTBotShell shell = bot.shell("New Project");
    shell.activate();

    SwtBotTreeUtilities.select(
        bot, bot.tree(), "Google Cloud Platform", "Google App Engine Standard Java Project");
    bot.button("Next >").click();

    if (extraBotActions != null) {
      extraBotActions.run();
    }

    bot.textWithLabel("Project name:").setText(projectName);
    if (location == null) {
      bot.checkBox("Use default location").select();
    } else {
      bot.checkBox("Use default location").deselect();
      bot.textWithLabel("Location:").setText(location);
    }
    if (javaPackage != null) {
      bot.textWithLabel("Java package:").setText(javaPackage);
    }
    if (runtime != null) {
      if (runtime == AppEngineRuntime.STANDARD_JAVA_8) {
        bot.comboBoxWithLabel("Java version:").setSelection("Java 8, Servlet 3.1");
      } else if (runtime == AppEngineRuntime.STANDARD_JAVA_8_SERVLET_25) {
        bot.comboBoxWithLabel("Java version:").setSelection("Java 8, Servlet 2.5");
      } else {
        Assert.fail("Runtime not handled: " + runtime);
      }
    }

    // can take a loooong time to resolve jars (e.g. servlet-api.jar) from Maven Central
    int libraryResolutionTimeout = 300 * 1000/* ms */;
    SwtBotTimeoutManager.setTimeout(libraryResolutionTimeout);
    try {
      SwtBotTestingUtilities.clickButtonAndWaitForWindowClose(bot, bot.button("Finish"));
    } catch (TimeoutException ex) {
      System.err.println("FATAL: timed out while waiting for the wizard to close. Forcibly killing "
          + "all shells: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1925");
      System.err.println("FATAL: You will see tons of related errors: \"Widget is disposed\", "
          + "\"Failed to execute runnable\", \"IllegalStateException\", etc.");
      SwtBotWorkbenchActions.killAllShells(bot);
      throw ex;
    }
    SwtBotTimeoutManager.resetTimeout();
    IProject project = waitUntilFacetedProjectExists(bot, getWorkspaceRoot().getProject(projectName));
    SwtBotWorkbenchActions.waitForProjects(bot, project);
    return project;
  }

  /**
   * Use the Eclipse general import project wizard to import an existing project from a
   * location.
   */
  public static IProject importNativeProject(SWTWorkbenchBot bot, String projectName,
      File extractedLocation) {

    openImportProjectsWizard(bot, "General", "Existing Projects into Workspace");
    bot.button("Next >").click();

    // current comboBox is associated with a radio button
    // with "Select root directory:"
    bot.comboBox().setText(extractedLocation.getAbsolutePath());
    bot.button("Refresh").click();

    // can take a loooong time to resolve jars (e.g. servlet-api.jar) from Maven Central
    int libraryResolutionTimeout = 300 * 1000/* ms */;
    SwtBotTimeoutManager.setTimeout(libraryResolutionTimeout);
    try {
      SwtBotTestingUtilities.clickButtonAndWaitForWindowClose(bot, bot.button("Finish"));
    } catch (TimeoutException ex) {
      System.err.println("FATAL: timed out while waiting for the wizard to close. Forcibly killing "
          + "all shells: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1925");
      System.err.println("FATAL: You will see tons of related errors: \"Widget is disposed\", "
          + "\"Failed to execute runnable\", \"IllegalStateException\", etc.");
      SwtBotWorkbenchActions.killAllShells(bot);
      throw ex;
    }
    SwtBotTimeoutManager.resetTimeout();
    IProject project =
        waitUntilFacetedProjectExists(bot, getWorkspaceRoot().getProject(projectName));
    SwtBotWorkbenchActions.waitForProjects(bot, project);
    return project;
  }

  private static void openImportProjectsWizard(
      SWTWorkbenchBot bot, String wizardCategory, String importWizardName) {
    bot.menu("File").menu("Import...").click();
    SWTBotShell shell = bot.shell("Import");
    shell.activate();
    SwtBotTreeUtilities.select(bot, bot.tree(), wizardCategory, importWizardName);
  }

  /** Import a Maven project from a zip file. */
  public static IProject importMavenProject(
      SWTWorkbenchBot bot, String projectName, File extractedLocation) {

    openImportProjectsWizard(bot, "Maven", "Existing Maven Projects");
    bot.button("Next >").click();

    bot.comboBoxWithLabel("Root Directory:").setText(extractedLocation.getAbsolutePath());
    bot.button("Refresh").click();

    try {
      SwtBotTestingUtilities.clickButtonAndWaitForWindowClose(bot, bot.button("Finish"));
    } catch (TimeoutException ex) {
      System.err.println("FATAL: timed out while waiting for the wizard to close. Forcibly killing "
          + "all shells: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1925");
      System.err.println("FATAL: You will see tons of related errors: \"Widget is disposed\", "
          + "\"Failed to execute runnable\", \"IllegalStateException\", etc.");
      SwtBotWorkbenchActions.killAllShells(bot);
      throw ex;
    }
    SwtBotTimeoutManager.resetTimeout();
    IProject project = waitUntilFacetedProjectExists(bot, getWorkspaceRoot().getProject(projectName));
    SwtBotWorkbenchActions.waitForProjects(bot, project);
    return project;
  }

  /**
   * Spin until the given project actually exists and is faceted.
   *
   * @return the project
   */
  private static IProject waitUntilFacetedProjectExists(SWTBot bot, IProject project) {
    bot.waitUntil(new DefaultCondition() {
      @Override
      public String getFailureMessage() {
        return "Project does not exist! " + project;
      }

      @Override
      public boolean test() throws Exception {
        return project.exists() && FacetedProjectFramework.isFacetedProject(project);
      }
    });
    return project;
  }

  private static IWorkspaceRoot getWorkspaceRoot() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  private SwtBotAppEngineActions() {}
}
