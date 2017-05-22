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

package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MavenCoordinateDialogPageUiTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();
  @Mock private DialogPage dialogPage;

  private Shell shell;
  private MavenCoordinateDialogPageUi mavenCoordinateUi;

  @Before
  public void setUp() {
    shell = shellResource.getShell();
    mavenCoordinateUi = new MavenCoordinateDialogPageUi(dialogPage);
  }

  @Test
  public void testUi_dynamicEnabling() {
    mavenCoordinateUi.createMavenCoordinatesArea(shell, true /* dynamic enabling */);
    assertNotNull(CompositeUtil.findControlAfterLabel(shell, Text.class, "Group ID:"));
    assertNotNull(CompositeUtil.findControlAfterLabel(shell, Text.class, "Artifact ID:"));
    assertNotNull(CompositeUtil.findControlAfterLabel(shell, Text.class, "Version:"));

    Button asMavenProject = CompositeUtil.findControl(shell, Button.class);
    assertEquals("Create as Maven project", asMavenProject.getText());
  }

  @Test
  public void testUi_noDynamicEnabling() {
    mavenCoordinateUi.createMavenCoordinatesArea(shell, false /* no dynamic enabling */);
    assertNotNull(CompositeUtil.findControlAfterLabel(shell, Text.class, "Group ID:"));
    assertNotNull(CompositeUtil.findControlAfterLabel(shell, Text.class, "Artifact ID:"));
    assertNotNull(CompositeUtil.findControlAfterLabel(shell, Text.class, "Version:"));

    assertNull(CompositeUtil.findControl(shell, Button.class));
  }

}
