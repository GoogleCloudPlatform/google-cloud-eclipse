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

import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.ContextMenuHelper;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

/**
 * SWTBot utility methods that perform general workbench actions.
 */
public final class SwtBotProjectActions {

  private static final String SOURCE_FOLDER = "src";

  /**
   * Creates a Java class with the specified name.
   *
   * @param bot The SWTWorkbenchBot
   * @param projectName The name of the project the class should be created in
   * @param packageName The name of the package the class should be created in
   * @param className The name of the class to be created
   */
  public static void createJavaClass(final SWTWorkbenchBot bot, String projectName,
      String packageName, final String className) {
    SWTBotTreeItem project = SwtBotProjectActions.selectProject(bot, projectName);
    selectProjectItem(project, SOURCE_FOLDER, packageName).select();
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
   * Creates a Java project with the specified name.
   *
   * @param bot the SWTWorkbenchBot
   * @param projectName the project name
   */
  public static void createJavaProject(SWTWorkbenchBot bot, String projectName) {
    // Open Java Perspective
    bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();

    // Open the list of new project wizards
    bot.menu("File").menu("New").menu("Project...").click();

    // Select the Java project
    SWTBotTree projectSelectionTree = bot.tree();
    SWTBotTreeItem projectSelectionGoogleTreeItem =
        SwtBotWorkbenchActions.getUniqueTreeItem(bot, projectSelectionTree, "Java", "Java Project");
    SwtBotTestingUtilities.selectTreeItem(bot, projectSelectionGoogleTreeItem, "Java Project");

    bot.button("Next >").click();

    // Configure the project and then create it
    bot.textWithLabel("Project name:").setText(projectName);

    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("Finish"));
  }

  /**
   * Create a Maven project from Archetype and land in the Java perspective.
   */
  public static void createMavenProjectFromArchetype(final SWTWorkbenchBot bot, String groupId,
      String artifactId, String packageName, String archetypeGroupId, String archetypeArtifactId,
      String archetypeVersion, String archetypeUrl) {
    // create maven project
    SwtBotMenuActions.openNewMavenProject(bot);

    // move to next step, archetype selection
    bot.button("Next >").click();

    // open archetype dialog
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        bot.button("Add Archetype...").click();
      }
    });

    // Dialog: "New Maven Project"
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

    // enable snapshots in table view
    bot.checkBox("Include snapshot archetypes").click();

    // filter so only one row shows up
    bot.text().setText("gwt-basic-archetype");

    // select first row
    SWTBotTable table = bot.table();
    table.setFocus();
    table.getTableItem(0).select();

    // move to last wizard
    bot.button("Next >").click();

    // set archetype inputs
    bot.comboBox(0).setText(groupId);
    bot.comboBox(1).setText(artifactId);
    bot.comboBox(3).setText(packageName);

    // finish and close dialog, and it will init
    bot.button("Finish").click();

    // change to the java perpective for next stage
    SwtBotMenuActions.openJavaPerpsective(bot);

    // select the first project
    bot.tree().setFocus();
  }

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
   * Returns true if the specified project can be found in the 'Package Explorer' or 'Project View',
   * otherwise returns false. Throws a WidgetNotFoundException exception if the 'Package Explorer'
   * or 'Project Explorer' view cannot be found.
   *
   * @param bot The SWTWorkbenchBot
   * @param projectName The name of the project to be found
   * @return true if the project is found, and false if not found
   */
  public static boolean doesProjectExist(final SWTWorkbenchBot bot, String projectName) {
    SWTBotView explorer = getExplorer(bot);

    // Select the root of the project tree in the explorer view
    Widget explorerWidget = explorer.getWidget();
    Tree explorerTree = bot.widget(widgetOfType(Tree.class), explorerWidget);
    SWTBotTreeItem[] allItems = new SWTBotTree(explorerTree).getAllItems();
    for (int i = 0; i < allItems.length; i++) {
      if (allItems[i].getText().equals(projectName)) {
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
      if (view.getTitle().equals("Package Explorer") || view.getTitle().equals("Project Explorer")) {
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

  /**
   * Returns true if there are errors in the Problem view. Returns false otherwise.
   */
  public static boolean hasErrorsInProblemsView(SWTWorkbenchBot bot) {
    // Open Problems View by Window -> show view -> Problems
    bot.menu("Window").menu("Show View").menu("Problems").click();

    SWTBotView view = bot.viewByTitle("Problems");
    view.show();
    SWTBotTree tree = view.bot().tree();

    for (SWTBotTreeItem item : tree.getAllItems()) {
      String text = item.getText();
      if (text != null && text.startsWith("Errors")) {
        return true;
      }
    }

    return false;
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
   * @param bot The SWTWorkbenchBot
   * @param projectName The project name
   */
  public static void refreshProject(final SWTWorkbenchBot bot, String projectName) {
    SWTBotTreeItem project = selectProject(bot, projectName);
    project.contextMenu("Refresh").click();
  }

  /**
   * Returns the specified project. Throws a WidgetNotFoundException if the 'Package Explorer' or
   * 'Project Explorer' view cannot be found or if the specified project cannot be found.
   *
   * @param bot The SWTWorkbenchBot
   * @param projectName The name of the project to select
   * @return
   */
  public static SWTBotTreeItem selectProject(final SWTWorkbenchBot bot, String projectName) {
    SWTBotView explorer = getExplorer(bot);

    // Select the root of the project tree in the explorer view
    Widget explorerWidget = explorer.getWidget();
    Tree explorerTree = bot.widget(widgetOfType(Tree.class), explorerWidget);
    return new SWTBotTree(explorerTree).getTreeItem(projectName).select();
  }

  /**
   * Select a file/folder by providing a parent tree, and a list folders that lead to the
   * file/folder.
   *
   * @param item Root tree item
   * @param folderPath List of folder names that lead to file
   * @return Returns a SWTBotTreeItem of the last name in texts
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
