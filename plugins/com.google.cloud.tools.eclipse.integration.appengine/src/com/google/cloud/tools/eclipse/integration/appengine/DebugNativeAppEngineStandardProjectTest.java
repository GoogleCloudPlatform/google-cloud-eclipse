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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.swtbot.SwtBotProjectActions;
import com.google.cloud.tools.eclipse.swtbot.SwtBotTestingUtilities;
import com.google.cloud.tools.eclipse.swtbot.SwtBotTreeUtilities;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.prefs.Preferences;

/**
 * Create a native App Engine Standard project, launch in debug mode, verify working, and then
 * terminate.
 */
public class DebugNativeAppEngineStandardProjectTest extends BaseProjectTest {

  private static final long TERMINATE_SERVER_TIMEOUT = 10000L;

  @Rule public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);
  
  /**
   * Launch a native application in debug mode and verify that:
   *
   * <ol>
   *   <li>it started,
   *   <li>it can be terminated and removed from the launch list, and
   *   <li>the process is actually terminated.
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

    project =
        SwtBotAppEngineActions.createNativeWebAppProject(
            bot, "testapp_java8", null, "app.engine.test", null /* runtime */);
    assertTrue(project.exists());

    SWTBotTreeItem testProject = SwtBotProjectActions.selectProject(bot, "testapp_java8");
    assertNotNull(testProject);
    SwtBotTestingUtilities.waitUntilMenuHasItem(
        bot, () -> bot.menu("Run").menu("Debug As"), endsWith("Debug on Server"));

    bot.menu("Run").menu("Debug As").menu("1 Debug on Server").click();
    SwtBotTestingUtilities.waitUntilShellIsOpen(bot, "Debug On Server");
    SWTBotShell debugPage = bot.shell("Debug On Server");
    debugPage.activate();
    SwtBotTestingUtilities.clickButtonAndWaitForWindowClose(bot, bot.button("Finish"));

    bot.perspectiveById("org.eclipse.debug.ui.DebugPerspective")
        .activate(); // IDebugUIConstants.ID_DEBUG_PERSPECTIVE
    SWTBotView debugView =
        bot.viewById("org.eclipse.debug.ui.DebugView"); // IDebugUIConstants.ID_DEBUG_VIEW
    debugView.show();

    SWTBotTree launchTree =
        new SWTBotTree(bot.widget(widgetOfType(Tree.class), debugView.getWidget()));

    // clean up any stray processes that may be lying around
    SWTBotMenu launchMenu = launchTree.contextMenu("Remove All Terminated");
    if (launchMenu.isEnabled()) {
      launchMenu.click();
    }

    SwtBotTreeUtilities.waitUntilTreeHasItems(bot, launchTree);
    SWTBotTreeItem[] allItems = launchTree.getAllItems();
    SwtBotTreeUtilities.waitUntilTreeContainsText(
        bot, allItems[0], "App Engine Standard at localhost");

    SWTBotView consoleView =
        bot.viewById("org.eclipse.ui.console.ConsoleView"); // IConsoleConstants.ID_CONSOLE_VIEW
    consoleView.show();
    SwtBotTestingUtilities.waitUntilViewContentDescription(
        bot, consoleView, Matchers.containsString("App Engine Standard at localhost"));
    SWTBotStyledText consoleContents =
        new SWTBotStyledText(bot.widget(widgetOfType(StyledText.class), consoleView.getWidget()));
    SwtBotTestingUtilities.waitUntilStyledTextContains(
        bot, "Module instance default is running at http://localhost:8080", consoleContents);

    // Server is now running
    assertEquals(
        "Hello App Engine!",
        getUrlContents(new URL("http://localhost:8080/hello"), (int) SWTBotPreferences.TIMEOUT));

    // Ensure debugger has connected by looking for well-known thread
    debugView.show();
    assertTrue(
        SwtBotTreeUtilities.hasChild(
            bot, launchTree, stringContainsInOrder(Arrays.asList("Thread", "(Running)"))));

    {
      SWTBotView serversView = bot.viewById("org.eclipse.wst.server.ui.ServersView");
      serversView.show();
      SWTBotTree serversTree =
          new SWTBotTree(bot.widget(widgetOfType(Tree.class), serversView.getWidget()));
      SwtBotTreeUtilities.waitUntilTreeHasItems(bot, serversTree);
      SWTBotTreeItem[] serverItems = serversTree.getAllItems();
      SwtBotTreeUtilities.waitUntilTreeContainsText(bot, serverItems[0], "[Debugging,");
    }

    // Stop the server
    SWTBotToolbarButton stopServerButton = null;
    for (SWTBotToolbarButton button : consoleView.getToolbarButtons()) {
      if ("Stop the server".equals(button.getToolTipText())) {
        stopServerButton = button;
        button.click();
      }
    }
    assertNotNull(stopServerButton);
    SwtBotTreeUtilities.waitUntilTreeTextMatches(
        bot, allItems[0], containsString("<terminated>"), TERMINATE_SERVER_TIMEOUT);
    assertNoService(new URL("http://localhost:8080/hello"));
    assertTrue(
        "App Engine console should mark as stopped",
        consoleView.getViewReference().getContentDescription().startsWith("<stopped>"));
    assertFalse("Stop Server button should be disabled", stopServerButton.isEnabled());

    // should also cause console to be discarded
    launchTree.contextMenu("Remove All Terminated").click();
    SwtBotTreeUtilities.waitUntilTreeHasNoItems(bot, launchTree);
    assertThat(
        "App Engine console should be removed",
        consoleView.getViewReference().getContentDescription(),
        Matchers.is("No consoles to display at this time."));
  }

  /** Check that there is no remote service for the URL. */
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
