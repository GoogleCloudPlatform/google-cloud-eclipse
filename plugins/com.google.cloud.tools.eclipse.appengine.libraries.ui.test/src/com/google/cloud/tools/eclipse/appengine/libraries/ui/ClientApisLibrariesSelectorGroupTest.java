/*
 * Copyright 2017 Google Inc.
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
import static org.junit.Assert.assertEquals;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ClientApisLibrariesSelectorGroupTest {

  @Rule public ShellTestResource shellTestResource = new ShellTestResource();

  private Shell shell;
  private LibrarySelectorGroup librariesSelector;
  private SWTBotCheckBox cloudStorageButton;
  private SWTBotCheckBox apiClientButton;
  private SWTBotCheckBox cloudCoreButton;

  @Before
  public void setUp() {
    shell = shellTestResource.getShell();
    shell.setLayout(new FillLayout());
    librariesSelector = new LibrarySelectorGroup(shell, CloudLibraries.CLIENT_APIS_GROUP, false);
    shell.open();
    cloudStorageButton = getButton("googlecloudstorage");
    apiClientButton = getButton("googleapiclient");
    cloudCoreButton = getButton("googlecloudcore");
  }

  @Test
  public void testButtonSetup() {
    Control groupAsControl = shell.getChildren()[0];
    assertThat(groupAsControl, instanceOf(Group.class));
    Control[] buttonsAsControls = ((Group) groupAsControl).getChildren();
    String[] expectedLibraryOrder = new String[] {"googleapiclient", "googlecloudcore",
        "googlecloudstorage", "clouddatastore", "cloudtranslation"};
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
    assertTrue(cloudStorageButton.getToolTipText().length() > 0);
    assertTrue(apiClientButton.getToolTipText().length() > 0);
    assertTrue(cloudCoreButton.getToolTipText().length() > 0);
  }

  @Test
  public void testInitiallyNoLibrariesSelected() {
    assertTrue(getSelectedLibrariesSorted().isEmpty());
  }

  @Test
  public void testSelectApiClient() {
    apiClientButton.click();
    List<Library> selectedLibraries = getSelectedLibrariesSorted();
    assertNotNull(selectedLibraries);
    assertEquals(1, selectedLibraries.size());
    assertEquals("googleapiclient", selectedLibraries.get(0).getId());
  }

  @Test
  public void testSelectCloudCore() {
    cloudCoreButton.click();
    List<Library> selectedLibraries = getSelectedLibrariesSorted();
    assertNotNull(selectedLibraries);
    assertEquals(2, selectedLibraries.size());
    assertEquals("googleapiclient", selectedLibraries.get(0).getId());
    assertEquals("googlecloudcore", selectedLibraries.get(1).getId());
  }
  
  @Test
  public void testSelectCloudStorage() {
    cloudStorageButton.click();
    List<Library> selectedLibraries = getSelectedLibrariesSorted();
    assertNotNull(selectedLibraries);
    assertEquals(4, selectedLibraries.size());
    assertEquals("appengine-api", selectedLibraries.get(0).getId());
    assertEquals("googleapiclient", selectedLibraries.get(1).getId());
    assertEquals("googlecloudcore", selectedLibraries.get(2).getId());
    assertEquals("googlecloudstorage", selectedLibraries.get(3).getId());
  }

  @Test
  public void testUnselectCloudStorage() {
    cloudStorageButton.click(); // select
    cloudStorageButton.click(); // unselect
    List<Library> selectedLibraries = getSelectedLibrariesSorted();
    assertNotNull(selectedLibraries);
    assertEquals(0, selectedLibraries.size());
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