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

package com.google.cloud.tools.eclipse.appengine.libraries.ui;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineLibrariesSelectorGroupTest {

  @Rule public ShellTestResource shellTestResource = new ShellTestResource();

  private Shell shell;
  private LibrarySelectorGroup librariesSelector;
  private SWTBotCheckBox sdkButton;
  private SWTBotCheckBox endpointsButton;
  private SWTBotCheckBox objectifyButton;

  @Before
  public void setUp() {
    shell = shellTestResource.getShell();
    shell.setLayout(new FillLayout());
    librariesSelector = new LibrarySelectorGroup(
        shell, CloudLibraries.APP_ENGINE_STANDARD_GROUP, "xxx"); //$NON-NLS-1$
    shell.open();
    sdkButton = getButton("appengine-api");
    endpointsButton = getButton("appengine-endpoints");
    objectifyButton = getButton("objectify");
  }

  @Test
  public void testButtonOrder() {
    Control container = shell.getChildren()[0];
    assertThat(container, instanceOf(Composite.class));
    Control groupAsControl = ((Composite) container).getChildren()[0];
    assertThat(groupAsControl, instanceOf(Group.class));
    Control[] buttonsAsControls = ((Group) groupAsControl).getChildren();
    String[] expectedLibraryOrder =
        new String[] {"appengine-api", "appengine-endpoints", "objectify"};
    for (int i = 0; i < buttonsAsControls.length; i++) {
      Control control = buttonsAsControls[i];
      assertThat(control, instanceOf(Button.class));
      Button button = (Button) control;
      assertNotNull(button.getData());
      assertThat(button.getData(), instanceOf(Library.class));
      Library library = (Library) button.getData();
      assertThat(library.getId(), is(expectedLibraryOrder[i]));
    }
  }

  @Test
  public void testToolTips() {
    assertTrue(sdkButton.getToolTipText().length() > 0);
    assertTrue(endpointsButton.getToolTipText().length() > 0);
    assertTrue(objectifyButton.getToolTipText().length() > 0);
  }

  @Test
  public void testInitiallyNoLibrariesSelected() {
    assertTrue(getSelectedLibrariesSorted().isEmpty());
  }

  @Test
  public void testSelectEndpointsSelectsAppEngineApiAsWell() {
    endpointsButton.click();
    List<Library> selectedLibraries = getSelectedLibrariesSorted();
    assertNotNull(selectedLibraries);
    assertThat(selectedLibraries.size(), is(2));
    assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
    assertThat(selectedLibraries.get(1).getId(), is("appengine-endpoints"));
  }

  @Test
  public void testSelectObjectifyDoesNotSelectAppEngineApiAsWell() {
    objectifyButton.click();
    List<Library> selectedLibraries = getSelectedLibrariesSorted();
    assertNotNull(selectedLibraries);
    assertThat(selectedLibraries.size(), is(1));
    assertThat(selectedLibraries.get(0).getId(), is("objectify"));
  }

  @Test
  public void testSelectObjectifyAndEndpointsSelectsAppEngineApiAsWell() {
    objectifyButton.click();
    endpointsButton.click();
    List<Library> selectedLibraries = getSelectedLibrariesSorted();
    assertNotNull(selectedLibraries);
    assertThat(selectedLibraries.size(), is(3));
    assertThat(selectedLibraries.get(0).getId(), is("appengine-api"));
    assertThat(selectedLibraries.get(1).getId(), is("appengine-endpoints"));
    assertThat(selectedLibraries.get(2).getId(), is("objectify"));
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

  private List<Library> getSelectedLibrariesSorted() {
    List<Library> selectedLibraries = new ArrayList<>(librariesSelector.getSelectedLibraries());
    Collections.sort(selectedLibraries, new LibraryComparator());
    return selectedLibraries;
  }
}
