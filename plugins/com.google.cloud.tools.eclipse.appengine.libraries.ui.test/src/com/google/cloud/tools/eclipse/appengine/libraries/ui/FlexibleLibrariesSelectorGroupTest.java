/*
 * Copyright 2018 Google LLC.
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FlexibleLibrariesSelectorGroupTest {

  @Rule public ShellTestResource shellTestResource = new ShellTestResource();

  private Shell shell;
  private LibrarySelectorGroup librariesSelector;
  private SWTBotCheckBox objectifyButton;

  @Before
  public void setUp() {
    shell = shellTestResource.getShell();
    shell.setLayout(new FillLayout());
    librariesSelector = new LibrarySelectorGroup(
        shell, CloudLibraries.APP_ENGINE_FLEXIBLE_GROUP, "xxx"); //$NON-NLS-1$
    shell.open();
    objectifyButton = getButton("objectify6");
  }

  @Test
  public void testToolTips() {
    assertTrue(objectifyButton.getToolTipText().length() > 0);
  }

  @Test
  public void testInitiallyNoLibrariesSelected() {
    assertTrue(getSelectedLibrariesSorted().isEmpty());
  }

  @Test
  public void testSelectObjectify() {
    objectifyButton.click();
    List<Library> selectedLibraries = getSelectedLibrariesSorted();
    assertNotNull(selectedLibraries);
    assertThat(selectedLibraries.size(), is(1));
    assertThat(selectedLibraries.get(0).getId(), is("objectify6"));
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
