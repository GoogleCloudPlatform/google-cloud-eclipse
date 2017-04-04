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

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.swtbot.SwtBotTestingUtilities;
import com.google.cloud.tools.eclipse.swtbot.SwtBotTreeUtilities;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.prefs.Preferences;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Create a native App Engine Standard project, launch in debug mode, verify working, and then
 * terminate.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class DebugNativeAppEngineStandardProjectTest extends BaseProjectTest {

  private static final long TERMINATE_SERVER_TIMEOUT = 10000L;

  @Rule
  public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  /**
   * Launch a native application in debug mode and verify that:
   * <ol>
   * <li>it started,</li>
   * <li>it can be terminated and removed from the launch list, and</li>
   * <li>the process is actually terminated.</li>
   * </ol>
   */
  @Test
  public void testDebugLaunch() throws Exception {
    // Disable WTP's download-server-bindings
    // Equivalent to: ServerUIPreferences.getInstance().setCacheFrequency(0);
    Preferences prefs = InstanceScope.INSTANCE.getNode("org.eclipse.wst.server.ui");
    prefs.putInt("cache-frequency", 0);
    prefs.sync();

    assertNoService(new URL("http://localhost:8080/hello"));

    project = SwtBotAppEngineActions.createNativeWebAppProject(bot, "testapp", null,
        "app.engine.test");
    assertTrue(project.exists());

    SWTBotTreeItem testappProject = SwtBotProjectActions.selectProject(bot, "testapp");
    assertNotNull(testappProject);
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        bot.menu("Run").menu("Debug As").menu("1 Debug on Server").click();
      }
    });

    SwtBotTestingUtilities.clickButtonAndWaitForWindowClose(bot, bot.button("Finish"));

    bot.perspectiveById("org.eclipse.debug.ui.DebugPerspective").activate(); // IDebugUIConstants.ID_DEBUG_PERSPECTIVE
    SWTBotView debugView = bot.viewById("org.eclipse.debug.ui.DebugView"); // IDebugUIConstants.ID_DEBUG_VIEW
    debugView.show();

    SWTBotTree launchTree =
        new SWTBotTree(bot.widget(widgetOfType(Tree.class), debugView.getWidget()));
    SwtBotTreeUtilities.waitUntilTreeHasItems(bot, launchTree);
    SWTBotTreeItem[] allItems = launchTree.getAllItems();
    SwtBotTreeUtilities.waitUntilTreeHasText(bot, allItems[0]);
    assertTrue("No App Engine launch found",
        allItems[0].getText().contains("App Engine Standard at localhost"));

    SWTBotView consoleView = bot.viewById("org.eclipse.ui.console.ConsoleView"); // IConsoleConstants.ID_CONSOLE_VIEW
    consoleView.show();
    assertTrue("App Engine console not active", consoleView.getViewReference()
        .getContentDescription().contains("App Engine Standard at localhost"));
    final SWTBotStyledText consoleContents =
        new SWTBotStyledText(bot.widget(widgetOfType(StyledText.class), consoleView.getWidget()));
    SwtBotTestingUtilities.waitUntilStyledTextContains(bot,
        "Module instance default is running at http://localhost:8080", consoleContents);

    assertEquals("Hello App Engine!",
        getUrlContents(new URL("http://localhost:8080/hello"), (int) SWTBotPreferences.TIMEOUT));

    SWTBotToolbarButton stopServerButton = null;
    for (SWTBotToolbarButton button : consoleView.getToolbarButtons()) {
      if ("Stop the server".equals(button.getToolTipText())) {
        stopServerButton = button;
        button.click();
      }
    }
    assertNotNull(stopServerButton);
    SwtBotTreeUtilities.waitUntilTreeContainsText(bot, allItems[0], "<terminated>",
                                                  TERMINATE_SERVER_TIMEOUT);
    assertNoService(new URL("http://localhost:8080/hello"));
    assertTrue("App Engine console should mark as stopped",
        consoleView.getViewReference().getContentDescription().startsWith("<stopped>"));
    assertFalse("Stop Server button should be disabled", stopServerButton.isEnabled());
  }

  /**
   * Check that there is no remote service for the URL.
   */
  private void assertNoService(URL url) {
    try {
      getUrlContents(url, 10);
      fail("There appears to be life at " + url);
    } catch (IOException ex) {
      /* expected */
    }
  }

  /**
   * Read the content as a string from the specified URL.
   * 
   * @throws IOException if cannot connect or timeout
   */
  private static String getUrlContents(URL url, int timeoutInMilliseconds) throws IOException {
    StringBuilder content = new StringBuilder();
    URLConnection conn = url.openConnection();
    conn.setConnectTimeout(100); // ms
    conn.setReadTimeout(timeoutInMilliseconds); // ms
    try (InputStreamReader reader =
        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
      char[] buf = new char[1024];
      int length;
      while ((length = reader.read(buf)) >= 0) {
        content.append(buf, 0, length);
      }
    }
    return content.toString().trim();
  }

}
