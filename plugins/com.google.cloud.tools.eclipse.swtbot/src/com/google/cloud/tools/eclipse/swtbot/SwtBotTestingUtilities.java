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

package com.google.cloud.tools.eclipse.swtbot;

import com.google.common.collect.Iterables;
import java.util.function.Supplier;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.hamcrest.Matcher;

/**
 * Provides helper methods to aid in SWTBot testing.
 */
public class SwtBotTestingUtilities {
  /**
   * The delay to use between a simulated key/button press's down and up events.
   */
  public static final int EVENT_DOWN_UP_DELAY_MS = 100;

  /** Click the button, wait for the window change. */
  public static void clickButtonAndWaitForWindowChange(SWTBot bot, SWTBotButton button) {
    performAndWaitForWindowChange(bot, button::click);
  }

  /** Click the button, wait for the window close. */
  public static void clickButtonAndWaitForWindowClose(SWTBot bot, SWTBotButton button) {
    performAndWaitForWindowClose(bot, button::click);
  }

  /**
   * Click on all table cells in column {@code col} with the contents {@code value}. Selection
   * should be the last cell clicked upon.
   */
  public static void clickOnTableCellValue(SWTBotTable table, int col, String value) {
    String column = table.columns().get(col);
    for (int row = 0; row < table.rowCount(); row++) {
      String cellValue = table.cell(row, column);
      if (cellValue.equals(value)) {
        table.click(row, col);
        break;
      }
    }
  }

  /**
   * Return true if the operating system is Mac.
   */
  public static boolean isMac() {
    String platform = SWT.getPlatform();
    return ("carbon".equals(platform) || "cocoa".equals(platform));
  }

  /**
   * Simple wrapper to block for actions that either open or close a window.
   */
  public static void performAndWaitForWindowChange(SWTBot bot, Runnable runnable) {
    SWTBotShell shell = bot.activeShell();
    runnable.run();
    waitUntilShellIsNotActive(bot, shell);
  }

  /**
   * Simple wrapper to block for actions that close a window.
   */
  public static void performAndWaitForWindowClose(SWTBot bot, Runnable runnable) {
    SWTBotShell shell = bot.activeShell();
    runnable.run();
    waitUntilShellIsClosed(bot, shell);
  }


  /**
   * Injects a key or character via down and up events. Only one of {@code keyCode} or
   * {@code character} must be provided. Use
   * 
   * @param keyCode the keycode of the key (use {@code 0} if unspecified)
   * @param character the character to press (use {@code '\0'} if unspecified)
   */
  public static void sendKeyDownAndUp(SWTBot bot, int keyCode, char character) {
    Event ev = new Event();
    ev.keyCode = keyCode;
    ev.character = character;
    ev.type = SWT.KeyDown;
    bot.getDisplay().post(ev);
    bot.sleep(EVENT_DOWN_UP_DELAY_MS);
    ev.type = SWT.KeyUp;
    bot.getDisplay().post(ev);
  }

  /**
   * Blocks the caller until the given shell is no longer active.
   */
  public static void waitUntilShellIsNotActive(SWTBot bot, SWTBotShell shell) {
    bot.waitUntil(new DefaultCondition() {
      @Override
      public String getFailureMessage() {
        return "Shell " + shell.getText() + " did not close"; //$NON-NLS-1$
      }

      @Override
      public boolean test() throws Exception {
        return !shell.isActive();
      }
    });
  }

  /**
   * Blocks the caller until the given shell is closed.
   */
  public static void waitUntilShellIsClosed(SWTBot bot, SWTBotShell shell) {
    bot.waitUntil(new DefaultCondition() {
      @Override
      public String getFailureMessage() {
        return "Shell " + shell.getText() + " did not close"; //$NON-NLS-1$
      }

      @Override
      public boolean test() throws Exception {
        return !shell.isOpen();
      }
    });
  }

  /**
   * Wait until the given text widget contains the provided string
   */
  public static void waitUntilStyledTextContains(SWTBot bot, String text, SWTBotStyledText widget) {
    bot.waitUntil(new DefaultCondition() {
      @Override
      public boolean test() throws Exception {
        return widget.getText().contains(text);
      }

      @Override
      public String getFailureMessage() {
        return "Text not found!";
      }
    });
  }

  /** Wait until the view's content description matches. */
  public static void waitUntilViewContentDescription(
      SWTBot bot, SWTBotView consoleView, Matcher<String> matcher) {
    bot.waitUntil(
        new DefaultCondition() {

          @Override
          public boolean test() throws Exception {
            return matcher.matches(consoleView.getViewReference().getContentDescription());
          }

          @Override
          public String getFailureMessage() {
            return matcher.toString();
          }
        });
  }

  /** Wait until a menu contains a matching item. */
  public static void waitUntilMenuHasItem(
      SWTBot bot, Supplier<SWTBotMenu> menuSupplier, Matcher<String> matcher) {
    bot.waitUntil(
        new DefaultCondition() {
          @Override
          public boolean test() throws Exception {
            return Iterables.any(menuSupplier.get().menuItems(), matcher::matches);
          }

          @Override
          public String getFailureMessage() {
            return "Never matched menu with " + matcher.toString();
          }
        });
  }
}
