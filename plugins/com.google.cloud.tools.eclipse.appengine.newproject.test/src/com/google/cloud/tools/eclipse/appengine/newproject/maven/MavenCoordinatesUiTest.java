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
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
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
  private MavenCoordinatesUi ui;

  @Before
  public void setUp() {
    shell = shellResource.getShell();
    ui = new MavenCoordinatesUi(shell, SWT.NONE);
  }

  @Test
  public void testDefaultFieldValues() {
    assertTrue(getGroupIdField().getText().isEmpty());
    assertTrue(getArtifactIdField().getText().isEmpty());
    assertEquals("0.1.0-SNAPSHOT", getVersionField().getText());
  }

  @Test
  public void testValidateMavenSettings_emptyGroupId() {
    IStatus status = ui.validateMavenSettings();
    assertEquals(status.getSeverity(), IStatus.INFO);
    assertEquals("Provide Maven Group ID.", status.getMessage());
  }

  @Test
  public void testValidateMavenSettings_emptyArtifactId() {
    getGroupIdField().setText("com.example");

    IStatus status = ui.validateMavenSettings();
    assertEquals(status.getSeverity(), IStatus.INFO);
    assertEquals("Provide Maven Artifact ID.", status.getMessage());
  }

  @Test
  public void testValidateMavenSettings_emptyVersion() {
    getGroupIdField().setText("com.example");
    getArtifactIdField().setText("some-artifact-id");
    getVersionField().setText("");

    IStatus status = ui.validateMavenSettings();
    assertEquals(status.getSeverity(), IStatus.INFO);
    assertEquals("Provide Maven artifact version.", status.getMessage());
  }

  @Test
  public void testValidateMavenSettings_illegalGroupId() {
    getArtifactIdField().setText("some-artifact-id");

    getGroupIdField().setText("<:#= Illegal ID =#:>");
    IStatus status = ui.validateMavenSettings();
    assertEquals(status.getSeverity(), IStatus.ERROR);
    assertEquals("Illegal Maven Group ID: <:#= Illegal ID =#:>", status.getMessage());
  }

  @Test
  public void testValidateMavenSettings_illegalArtifactId() {
    getGroupIdField().setText("com.example");

    getArtifactIdField().setText("<:#= Illegal ID =#:>");
    IStatus status = ui.validateMavenSettings();
    assertEquals(status.getSeverity(), IStatus.ERROR);
    assertEquals("Illegal Maven Artifact ID: <:#= Illegal ID =#:>", status.getMessage());
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
