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

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.hamcrest.Matcher;

/**
 * SWTBot utility methods that perform general file actions.
 */
public final class SwtBotMenuActions {

  public static String getEditorText(final SWTWorkbenchBot bot, String title) {
    SWTBotEclipseEditor textEditor = bot.editorByTitle(title).toTextEditor();
    return textEditor.getText();
  }

  /**
   * Opens the Debug Configurations... perspective.
   */
  public static void openDebugConfiguration(final SWTBot bot) {
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        // Be sure the main window has focus
        setFocusMainShell(bot);

        bot.menu("Run").menu("Debug Configurations...").click();
      }
    });
  }

  public static void openDebugPerspective(final SWTBot bot) {
    // Set timeout low, in case the shell is not a workbench window
    SwtBotTimeoutManager.setTimeout(500);
    try {
      bot.menu("Window").menu("Open Perspective").menu("Debug").click();
    } catch (WidgetNotFoundException e) {
      // Try "Other" in case we are in a perspective that doesn't list Debug
      // as a perspective shortcut, such as the Resource perspective.
      bot.menu("Window").menu("Open Perspective").menu("Other...");
      bot.list().select("Debug");
      bot.button("OK").click();
    }
    // Restore original timeout
    SwtBotTimeoutManager.setTimeout();
  }

  /**
   * Open debug perspective and terminate the process and wait for it be terminated.
   */
  public static void openDebugPerspectiveAndTerminateProcess(SWTWorkbenchBot bot) {
    openDebugPerspective(bot);

    // Right click and terminate thread
    @SuppressWarnings("rawtypes")
    Matcher matcher = WidgetMatcherFactory.withPartName("Debug");
    @SuppressWarnings("unchecked")
    SWTBotView debug = bot.view(matcher);
    final SWTBotTree tree = debug.bot().tree();
    SWTBotTreeItem[] items = tree.getAllItems();
    if (items.length > 0) {
      SWTBotTreeItem first = items[0];
      first.contextMenu("Terminate").click();

      // Wait for process to spin down
      bot.waitUntil(new DefaultCondition() {
        @Override
        public String getFailureMessage() {
          return "Couldn't find terminated process.";
        }

        @Override
        public boolean test() throws Exception {
          SWTBotTreeItem[] items = tree.getAllItems();
          return items[0].getText().contains("terminated");
        }
      });
    }
  }

  public static void openJavaPerpsective(SWTBot bot) {
    bot.menu("Window").menu("Open Perspective").menu("Java").click();
  }

  public static void openNewMavenProject(SWTWorkbenchBot bot) {
    openNewOtherProjectDialog(bot);

    // filter maven options
    bot.text().setText("maven");
    bot.sleep(500);

    // click on Maven Project
    SWTBotTree tree = bot.tree();
    SWTBotTreeItem[] items = tree.getAllItems();
    SwtBotTestingUtilities.selectTreeItem(bot, items[0], "Maven Project");

    // move to next step
    bot.button("Next >").click();
  }

  public static void openNewOtherProjectDialog(final SWTWorkbenchBot bot) {
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        bot.menu("File").menu("New").menu("Other...").click();
      }
    });
  }

  /**
   * Opens a resource using the Open Resource dialog.
   */
  public static void openResource(final SWTBot bot, String fileName) {
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        bot.menu("Navigate").menu("Open Resource").click();
      }
    });

    bot.text().typeText(fileName);
    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("Open"));
  }

  public static void openViewSelections(final SWTWorkbenchBot bot) {
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        bot.menu("Window").menu("Show View").menu("Other...").click();
      }
    });
  }

  /**
   * Set focus on the main shell window.
   */
  public static void setFocusMainShell(SWTBot bot) {
    SWTBotShell shell = bot.shells()[0];
    shell.setFocus();
  }

  /**
   * Show the console view. Be sure that perspective has the option first.
   */
  public static void showConsole(SWTWorkbenchBot bot) {
    bot.menu("Window").menu("Show View").menu("Console");
  }

  private SwtBotMenuActions() {}

}
