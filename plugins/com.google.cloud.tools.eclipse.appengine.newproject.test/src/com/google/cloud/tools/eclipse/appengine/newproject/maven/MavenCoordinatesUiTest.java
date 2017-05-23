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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
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
public class MavenCoordinatesUiTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();
  @Mock private DialogPage dialogPage;

  private Shell shell;

  @Before
  public void setUp() {
    shell = shellResource.getShell();
  }

  @Test
  public void testUiWithDynamicEnabling() {
    new MavenCoordinatesUi(shell, true /* dynamic enabling */);

    Button asMavenProject = CompositeUtil.findControl(shell, Button.class);
    assertEquals("Create as Maven project", asMavenProject.getText());

    assertFalse(asMavenProject.getSelection());
    assertFalse(getGroupIdField().getEnabled());
    assertFalse(getArtifactIdField().getEnabled());
    assertFalse(getVersionField().getEnabled());
  }

  @Test
  public void testUiWithNoDynamicEnabling() {
    new MavenCoordinatesUi(shell, false /* no dynamic enabling */);

    assertNull(CompositeUtil.findControl(shell, Button.class));

    assertTrue(getGroupIdField().getEnabled());
    assertTrue(getArtifactIdField().getEnabled());
    assertTrue(getVersionField().getEnabled());
  }

  @Test
  public void testDefaultFieldValues() {
    new MavenCoordinatesUi(shell, false);
    assertTrue(getGroupIdField().getText().isEmpty());
    assertTrue(getArtifactIdField().getText().isEmpty());
    assertEquals("0.1.0-SNAPSHOT", getVersionField().getText());
  }

  @Test
  public void testDynamicEnabling() {
    new MavenCoordinatesUi(shell, true /* dynamic enabling */);
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
  public void testValidateMavenSettings_emptyGroupId() {
    MavenCoordinatesUi ui = new MavenCoordinatesUi(shell, false);

    assertFalse(ui.setValidationMessage(dialogPage));
    verify(dialogPage).setMessage("Provide Maven Group ID.", IMessageProvider.INFORMATION);
  }

  @Test
  public void testValidateMavenSettings_emptyArtifactId() {
    MavenCoordinatesUi ui = new MavenCoordinatesUi(shell, false);
    getGroupIdField().setText("com.example");

    assertFalse(ui.setValidationMessage(dialogPage));
    verify(dialogPage).setMessage("Provide Maven Artifact ID.", IMessageProvider.INFORMATION);
  }

  @Test
  public void testValidateMavenSettings_emptyVersion() {
    MavenCoordinatesUi ui = new MavenCoordinatesUi(shell, false);
    getGroupIdField().setText("com.example");
    getArtifactIdField().setText("some-artifact-id");
    getVersionField().setText("");

    assertFalse(ui.setValidationMessage(dialogPage));
    verify(dialogPage).setMessage("Provide Maven artifact version.", IMessageProvider.INFORMATION);
  }

  @Test
  public void testValidateMavenSettings_illegalGroupId() {
    MavenCoordinatesUi ui = new MavenCoordinatesUi(shell, false);
    getArtifactIdField().setText("some-artifact-id");

    getGroupIdField().setText("<:#= Illegal ID =#:>");
    assertFalse(ui.setValidationMessage(dialogPage));
    verify(dialogPage).setErrorMessage("Illegal Maven Group ID: <:#= Illegal ID =#:>");
  }

  @Test
  public void testValidateMavenSettings_illegalArtifactId() {
    MavenCoordinatesUi ui = new MavenCoordinatesUi(shell, false);
    getGroupIdField().setText("com.example");

    getArtifactIdField().setText("<:#= Illegal ID =#:>");
    assertFalse(ui.setValidationMessage(dialogPage));
    verify(dialogPage).setErrorMessage("Illegal Maven Artifact ID: <:#= Illegal ID =#:>");
  }

  @Test
  public void testValidateMavenSettings_noValidationIfUiDisabled() {
    MavenCoordinatesUi ui = new MavenCoordinatesUi(shell, true /* dynamic enabling */);

    getGroupIdField().setText("<:#= Illegal ID =#:>");
    assertTrue(ui.setValidationMessage(dialogPage));
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
