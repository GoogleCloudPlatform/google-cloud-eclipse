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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MavenCoordinatesWizardUiTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();
  @Mock private DialogPage dialogPage;

  private Shell shell;
  private MavenCoordinatesWizardUi ui;

  @Before
  public void setUp() {
    shell = shellResource.getShell();
    ui = new MavenCoordinatesWizardUi(shell, SWT.NONE);
  }

  @Test
  public void testUi() {
    Button asMavenProject = CompositeUtil.findControl(shell, Button.class);
    assertEquals("Create as Maven project", asMavenProject.getText());

    assertFalse(asMavenProject.getSelection());
    assertFalse(getGroupIdField().getEnabled());
    assertFalse(getArtifactIdField().getEnabled());
    assertFalse(getVersionField().getEnabled());
  }

  @Test
  public void testDefaultFieldValues() {
    assertTrue(getGroupIdField().getText().isEmpty());
    assertTrue(getArtifactIdField().getText().isEmpty());
    assertEquals("0.1.0-SNAPSHOT", getVersionField().getText());
  }

  @Test
  public void testDynamicEnabling() {
    Button asMavenProject = CompositeUtil.findControl(shell, Button.class);

    new SWTBotCheckBox(asMavenProject).click();
    assertTrue(getGroupIdField().getEnabled());
    assertTrue(getArtifactIdField().getEnabled());
    assertTrue(getVersionField().getEnabled());

    new SWTBotCheckBox(asMavenProject).click();
    assertFalse(getGroupIdField().getEnabled());
    assertFalse(getArtifactIdField().getEnabled());
    assertFalse(getVersionField().getEnabled());
  }

  @Test
  public void testValidateMavenSettings_okWhenDisabled() {
    assertFalse(ui.uiEnabled());
    assertTrue(ui.validateMavenSettings().isOK());
  }

  private Text getGroupIdField() {
    return CompositeUtil.findControlAfterLabel(shell, Text.class, "Group ID:");
  }

  private Text getArtifactIdField() {
    return CompositeUtil.findControlAfterLabel(shell, Text.class, "Artifact ID:");
  }

  private Text getVersionField() {
    return CompositeUtil.findControlAfterLabel(shell, Text.class, "Version:");
  }
}
