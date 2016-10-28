/*
7 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AppEngineLibrariesSelectorGroupTest {

  private Shell shell;
  private AppEngineLibrariesSelectorGroup librariesSelector;
  private SWTBotCheckBox appengineButton;
  private SWTBotCheckBox endpointsButton;
  private SWTBotCheckBox objectifyButton;

  @Before
  public void setUp() throws Exception {
    Display.getDefault().syncExec(new Runnable() {

      @Override
      public void run() {
        shell = new Shell(Display.getDefault());
        shell.setLayout(new FillLayout());
        librariesSelector = new AppEngineLibrariesSelectorGroup(shell);
        shell.open();
        appengineButton = getButton("appengine-api");
        endpointsButton = getButton("appengine-endpoints");
        objectifyButton = getButton("objectify");
      }
    });
  }

  @After
  public void tearDown() {
    Display.getDefault().syncExec(new Runnable() {

      @Override
      public void run() {

        if (shell != null) {
          shell.dispose();
        }
      }
    });
  }

  @Test
  public void testInitiallyNoLibrariesSelected() {
    Display.getDefault().syncExec(new Runnable() {

      @Override
      public void run() {
        assertTrue(librariesSelector.getSelectedLibraries().isEmpty());
      }
    });
  }

  @Test
  public void testSelectAppEngineApi() {
    Display.getDefault().syncExec(new Runnable() {

      @Override
      public void run() {

        appengineButton.click();
        List<Library> selectedLibraries = librariesSelector.getSelectedLibraries();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(1));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
      }
    });
  }

  @Test
  public void testSelectEndpointsSelectsAppEngineApiAsWell() {
    Display.getDefault().syncExec(new Runnable() {

      @Override
      public void run() {
        endpointsButton.click();
        List<Library> selectedLibraries = librariesSelector.getSelectedLibraries();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(2));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
        assertThat(selectedLibraries.get(1).getId(), is("appengine-endpoints"));
      }});
  }

  @Test
  public void testSelectObjectifySelectsAppEngineApiAsWell() {
    Display.getDefault().syncExec(new Runnable() {

      @Override
      public void run() {
        objectifyButton.click();
        List<Library> selectedLibraries = librariesSelector.getSelectedLibraries();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(2));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
        assertThat(selectedLibraries.get(1).getId(), is("objectify"));
      }});
  }

  @Test
  public void testSelectObjectifyAndEndpointsSelectsAppEngineApiAsWell() {
    Display.getDefault().syncExec(new Runnable() {

      @Override
      public void run() {
        objectifyButton.click();
        endpointsButton.click();
        List<Library> selectedLibraries = librariesSelector.getSelectedLibraries();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(3));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
        assertThat(selectedLibraries.get(1).getId(), is("appengine-endpoints"));
        assertThat(selectedLibraries.get(2).getId(), is("objectify"));
      }});
  }

  @Test
  public void testSelectObjectifyAndEndpointsThenUnselectObjectifyShouldKeepAppEngineApiSelected() {
    Display.getDefault().syncExec(new Runnable() {

      @Override
      public void run() {
        objectifyButton.click();
        endpointsButton.click();
        objectifyButton.click();
        List<Library> selectedLibraries = librariesSelector.getSelectedLibraries();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(2));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
        assertThat(selectedLibraries.get(1).getId(), is("appengine-endpoints"));
      }});
  }

  @Test
  public void testSelectObjectifyAndEndpointsThenUnselectEndpointsShouldKeepAppEngineApiSelected() {
    Display.getDefault().syncExec(new Runnable() {

      @Override
      public void run() {
        endpointsButton.click();
        objectifyButton.click();
        endpointsButton.click();
        List<Library> selectedLibraries = librariesSelector.getSelectedLibraries();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(2));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
        assertThat(selectedLibraries.get(1).getId(), is("objectify"));
      }});
  }

  @Test
  public void testSelectObjectifyAndEndpointsThenUnselectBothShouldMakeAppEngineApiUnSelected() {
    Display.getDefault().syncExec(new Runnable() {

      @Override
      public void run() {
        objectifyButton.click();
        endpointsButton.click();
        objectifyButton.click();
        endpointsButton.click();
        List<Library> selectedLibraries = librariesSelector.getSelectedLibraries();
        assertNotNull(selectedLibraries);
        assertTrue(selectedLibraries.isEmpty());
      }});
  }

  /**
   * @param focusout
   */
  static void notify(int eventType, Widget widget, Display display) {
    Event event = new Event();
    event.time = (int) System.currentTimeMillis();
    event.widget = widget;
    event.display = display;
    event.type = eventType;
    if (eventType == SWT.Selection) {
      event.stateMask = 524288;
    }
    widget.notifyListeners(eventType, event);
    while (!display.isDisposed() && display.readAndDispatch()) {;}
  }

  private SWTBotCheckBox getButton(String libraryId) {
    for (Button button : librariesSelector.getLibraryButtons()) {
      if (libraryId.equals(((Library) button.getData()).getId())) {
        return new SWTBotCheckBox(button);
      }
    }
    fail("Could not find button for " + libraryId);
    return null; // won't be reached
  }

}
