/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.eclipse.swtbot;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.ContextMenuHelper;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

import java.util.ArrayList;
import java.util.List;

/**
 * SWTBot utility methods that perform general workbench actions.
 */
public final class SwtBotProjectActions {

  /**
   * Creates a Java class with the specified name.
   *
   * @param projectName the name of the project the class should be created in
   * @param sourceFolder the name of the source folder in which the class should be created.
   *        Typically "src" for normal Java projects, or "src/main/java" for Maven projects
   * @param packageName the name of the package the class should be created in
   * @param className the name of the class to be created
   */
  public static void createJavaClass(final SWTWorkbenchBot bot, String sourceFolder,
      String projectName,
      String packageName, final String className) {
    SWTBotTreeItem project = SwtBotProjectActions.selectProject(bot, projectName);
    selectProjectItem(project, sourceFolder, packageName).select();
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        MenuItem menuItem = ContextMenuHelper.contextMenu(getProjectRootTree(bot), "New", "Class");
        new SWTBotMenu(menuItem).click();
      }
    });

    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        bot.activeShell();
        bot.textWithLabel("Name:").setText(className);
        SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("Finish"));
      }
    });
  }

  /**
   * Create a Maven project based on an archetype.
   */
  public static IProject createMavenProject(final SWTWorkbenchBot bot, String groupId,
      String artifactId, String archetypeGroupId, String archetypeArtifactId,
      String archetypeVersion, String archetypeUrl, String javaPackage) {
    bot.menu("File").menu("New").menu("Project...").click();

    SWTBotShell shell = bot.shell("New Project");
    shell.activate();

    bot.tree().expandNode("Maven").select("Maven Project");
    bot.button("Next >").click();

    // we want to specify an archetype
    bot.checkBox("Create a simple project (skip archetype selection)").deselect();
    bot.button("Next >").click();

    // open archetype dialog
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        bot.button("Add Archetype...").click();
      }
    });

    bot.comboBox(0).setText(archetypeGroupId);
    bot.comboBox(1).setText(archetypeArtifactId);
    bot.comboBox(2).setText(archetypeVersion);
    bot.comboBox(3).setText(archetypeUrl);

    // close archetype dialog
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        // After OK, it will take a minute to download
        bot.button("OK").click();
      }
    });

    // move to last wizard
    bot.button("Next >").click();

    // set archetype inputs
    bot.comboBoxWithLabel("Group Id:").setText(groupId);
    bot.comboBoxWithLabel("Artifact Id:").setText(artifactId);
    bot.comboBoxWithLabel("Package:").setText(javaPackage);

    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("Finish"));
    return getWorkspaceRoot().getProject("testartifact");
  }

  private static IWorkspaceRoot getWorkspaceRoot() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  /**
   * Delete the specified project using the delete option from the project context menu.
   * 
   * @param projectName the name of the project
   */
  public static void deleteProject(final SWTWorkbenchBot bot, final String projectName) {
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        selectProject(bot, projectName).contextMenu("Delete").click();
        // Wait for confirmation window to come up
      }
    });

    // Select the "Delete project contents on disk (cannot be undone)"
    bot.checkBox(0).click();

    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("OK"));
  }

  /**
   * Returns true if the specified project is found in the 'Package Explorer' or 'Project View',
   * otherwise returns false. Throws a WidgetNotFoundException exception if the 'Package Explorer'
   * or 'Project Explorer' view cannot be found.
   *
   * @param projectName the name of the project to be found
   * @return true if the project is found, and false if not found
   */
  public static boolean projectFound(final SWTWorkbenchBot bot, String projectName) {
    SWTBotView explorer = getExplorer(bot);

    // Select the root of the project tree in the explorer view
    Widget explorerWidget = explorer.getWidget();
    Tree explorerTree = bot.widget(widgetOfType(Tree.class), explorerWidget);
    for (SWTBotTreeItem item : new SWTBotTree(explorerTree).getAllItems()) {
      if (item.getText().equals(projectName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Choose either the Package Explorer View or the Project Explorer view. Some perspectives have
   * the Package Explorer View open by default, whereas others use the Project Explorer View.
   * 
   * @throws WidgetNoFoundException if an explorer is not found
   */
  public static SWTBotView getExplorer(final SWTWorkbenchBot bot) {
    for (SWTBotView view : bot.views()) {
      if (view.getTitle().equals("Package Explorer")
          || view.getTitle().equals("Project Explorer")) {
        return view;
      }
    }
    throw new WidgetNotFoundException(
        "Could not find the 'Package Explorer' or 'Project Explorer' view.");
  }

  /**
   * Returns the project root tree in Package Explorer.
   */
  public static SWTBotTree getProjectRootTree(SWTWorkbenchBot bot) {
    SWTBotView explorer = getExplorer(bot);
    Tree tree = bot.widget(widgetOfType(Tree.class), explorer.getWidget());
    return new SWTBotTree(tree);
  }

  /** Return the list of all problems in the workspace. */
  public static List<String> getAllBuildErrors(SWTWorkbenchBot bot) {
    IWorkspaceRoot root = getWorkspaceRoot();
    List<String> foundProblems = new ArrayList<>();
    for (IProject project : root.getProjects()) {
      try {
        IMarker[] problems = project.findMarkers(IMarker.PROBLEM, true /* includeSubtypes */,
            IResource.DEPTH_INFINITE);
        for (IMarker problem : problems) {
          int severity = problem.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
          if (severity >= IMarker.SEVERITY_ERROR) {
            foundProblems.add(formatProblem(problem));
          }
        }
      } catch (CoreException ex) {
        // re-throw as we shouldn't have problems in tests
        throw new RuntimeException(ex);
      }
    }
    return foundProblems;
  }

  private static String formatProblem(IMarker problem) {
    StringBuilder sb = new StringBuilder();
    sb.append(problem.getResource().getFullPath());
    sb.append(':');
    sb.append(problem.getAttribute(IMarker.LINE_NUMBER, -1));
    sb.append(": ");
    sb.append(problem.getAttribute(IMarker.MESSAGE, ""));
    return sb.toString();
  }

  /**
   * Opens the Properties dialog for a given project.
   *
   * This method assumes that either the Package Explorer or Project Explorer view is visible.
   */
  public static void openProjectProperties(final SWTWorkbenchBot bot, String projectName) {
    selectProject(bot, projectName);

    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        // Open the Project Properties menu via the File menu
        SWTBotMenu fileMenu = bot.menu("File");
        fileMenu.menu("Properties").click();
      }
    });
  }

  /**
   * Refresh project tree.
   *
   * @param projectName the project name
   */
  public static void refreshProject(final SWTWorkbenchBot bot, String projectName) {
    SWTBotTreeItem project = selectProject(bot, projectName);
    project.contextMenu("Refresh").click();
  }

  /**
   * Returns the specified project.
   *
   * @param projectName the name of the project to select
   * @return the selected tree item
   * @throws WidgetNoFoundException if the 'Package Explorer' or 'Project Explorer' view cannot be
   *         found or if the specified project cannot be found.
   */
  public static SWTBotTreeItem selectProject(final SWTWorkbenchBot bot, String projectName) {
    SWTBotView explorer = getExplorer(bot);

    // Select the root of the project tree in the explorer view
    Widget explorerWidget = explorer.getWidget();
    Tree explorerTree = bot.widget(widgetOfType(Tree.class), explorerWidget);
    return new SWTBotTree(explorerTree).getTreeItem(projectName).select();
  }

  /**
   * Select a file/folder by providing a parent tree, and a list of folders that leads to the
   * file/folder.
   *
   * @param item root tree item
   * @param folderPath list of folder names that lead to file
   * @return the SWTBotTreeItem of the final selected item, or {@code null} if not found
   */
  public static SWTBotTreeItem selectProjectItem(SWTBotTreeItem item, String... folderPath) {
    for (String folder : folderPath) {
      if (item == null) {
        return null;
      }
      item.doubleClick();
      item = item.getNode(folder);
    }
    return item;
  }

  private SwtBotProjectActions() {}

}
