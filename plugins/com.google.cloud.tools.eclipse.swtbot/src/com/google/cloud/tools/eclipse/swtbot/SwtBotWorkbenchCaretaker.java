/*
 * Copyright 2018 Google LLC
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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/** Take screenshot after test failure, and clean up the workbench. */
public class SwtBotWorkbenchCaretaker extends TestWatcher {
  private static final Logger logger = Logger.getLogger(SwtBotWorkbenchCaretaker.class.getName());

  @Override
  protected void starting(Description description) {
    try {
      SwtBotWorkbenchActions.closeWelcome(new SWTWorkbenchBot());
    } catch (WidgetNotFoundException ex) {
      // may receive WNFE: "There is no active view"
    }
  }

  @Override
  protected void failed(Throwable e, Description description) {
    // use same naming convention as ScreenshotCaptureListener
    String fileName =
        SWTBotPreferences.SCREENSHOTS_DIR
            + "/"
            + description.getDisplayName()
            + "."
            + SWTBotPreferences.SCREENSHOT_FORMAT.toLowerCase();
    SWTUtils.captureScreenshot(fileName);
    logger.log(Level.INFO, "Screenshot saved as " + fileName);
  }

  @Override
  protected void succeeded(Description description) {
    SwtBotWorkbenchActions.resetWorkbench(new SWTWorkbenchBot());
  }
}
