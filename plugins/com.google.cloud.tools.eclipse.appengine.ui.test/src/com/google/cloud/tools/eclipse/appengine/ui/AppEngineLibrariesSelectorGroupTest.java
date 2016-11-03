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
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineLibrariesSelectorGroupTest {

  @Rule public ShellTestResource shellTestResource = new ShellTestResource();

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
        shell = shellTestResource.getShell();
        shell.setLayout(new FillLayout());
        librariesSelector = new AppEngineLibrariesSelectorGroup(shell);
        shell.open();
        appengineButton = getButton("appengine-api");
        endpointsButton = getButton("appengine-endpoints");
        objectifyButton = getButton("objectify");
      }
    });
  }

  @Test
  public void testInitiallyNoLibrariesSelected() {
    syncExec(new Runnable() {

      @Override
      public void run() {
        assertTrue(getSelectedLibrariesSorted().isEmpty());
      }
    });
  }

  @Test
  public void testSelectAppEngineApi() {
    syncExec(new Runnable() {

      @Override
      public void run() {
        appengineButton.click();
        List<Library> selectedLibraries = getSelectedLibrariesSorted();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(1));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
      }
    });
  }

  @Test
  public void testSelectEndpointsSelectsAppEngineApiAsWell() {
    syncExec(new Runnable() {

      @Override
      public void run() {
        endpointsButton.click();
        List<Library> selectedLibraries = getSelectedLibrariesSorted();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(2));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
        assertThat(selectedLibraries.get(1).getId(), is("appengine-endpoints"));
      }});
  }

  @Test
  public void testSelectObjectifySelectsAppEngineApiAsWell() {
    syncExec(new Runnable() {

      @Override
      public void run() {
        objectifyButton.click();
        List<Library> selectedLibraries = getSelectedLibrariesSorted();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(2));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
        assertThat(selectedLibraries.get(1).getId(), is("objectify"));
      }});
  }

  @Test
  public void testSelectObjectifyAndEndpointsSelectsAppEngineApiAsWell() {
    syncExec(new Runnable() {

      @Override
      public void run() {
        objectifyButton.click();
        endpointsButton.click();
        List<Library> selectedLibraries = getSelectedLibrariesSorted();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(3));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
        assertThat(selectedLibraries.get(1).getId(), is("appengine-endpoints"));
        assertThat(selectedLibraries.get(2).getId(), is("objectify"));
      }});
  }

  @Test
  public void testSelectObjectifyAndEndpointsThenUnselectObjectifyShouldKeepAppEngineApiSelected() {
    syncExec(new Runnable() {

      @Override
      public void run() {
        objectifyButton.click();
        endpointsButton.click();
        objectifyButton.click();
        List<Library> selectedLibraries = getSelectedLibrariesSorted();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(2));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
        assertThat(selectedLibraries.get(1).getId(), is("appengine-endpoints"));
      }});
  }

  @Test
  public void testSelectObjectifyAndEndpointsThenUnselectEndpointsShouldKeepAppEngineApiSelected() {
    syncExec(new Runnable() {

      @Override
      public void run() {
        endpointsButton.click();
        objectifyButton.click();
        endpointsButton.click();
        List<Library> selectedLibraries = getSelectedLibrariesSorted();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(2));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
        assertThat(selectedLibraries.get(1).getId(), is("objectify"));
      }});
  }

  @Test
  public void testSelectObjectifyAndEndpointsThenUnselectBothShouldMakeAppEngineApiUnSelected() {
    syncExec(new Runnable() {

      @Override
      public void run() {
        objectifyButton.click();
        endpointsButton.click();
        objectifyButton.click();
        endpointsButton.click();
        List<Library> selectedLibraries = getSelectedLibrariesSorted();
        assertNotNull(selectedLibraries);
        assertTrue(selectedLibraries.isEmpty());
      }});
  }

  @Test
  public void testSelectAppengineApiThenEndpointsThenUnselectEndpointsShouldKeepAppEngineSelected() {
    syncExec(new Runnable() {

      @Override
      public void run() {
        appengineButton.click();
        endpointsButton.click();
        endpointsButton.click();
        List<Library> selectedLibraries = getSelectedLibrariesSorted();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(1));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
      }});
  }

  @Test
  public void testSelectAppengineApiThenObjectifyThenUnselectObjectifyShouldKeepAppEngineSelected() {
    syncExec(new Runnable() {

      @Override
      public void run() {
        appengineButton.click();
        objectifyButton.click();
        objectifyButton.click();
        List<Library> selectedLibraries = getSelectedLibrariesSorted();
        assertNotNull(selectedLibraries);
        assertThat(selectedLibraries.size(), is(1));
        assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
      }});
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

  private void syncExec(Runnable runnable) {
    shell.getDisplay().syncExec(runnable);
  }

  private List<Library> getSelectedLibrariesSorted() {
    List<Library> selectedLibraries = librariesSelector.getSelectedLibraries();
    Collections.sort(selectedLibraries, new Comparator<Library>() {

      @Override
      public int compare(Library l1, Library l2) {
        return l1.getId().compareTo(l2.getId());
      }
    });
    return selectedLibraries;
  }
}
