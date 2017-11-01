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

package com.google.cloud.tools.eclipse.appengine.newproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.newproject.maven.MavenCoordinatesInput;
import com.google.cloud.tools.eclipse.appengine.newproject.maven.MavenCoordinatesWizardUi;
import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.common.base.Predicate;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineWizardPageTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  private AppEngineWizardPage page;
  private MavenCoordinatesInput mavenCoordinatesInput;

  @Before
  public void setUp() {
    page = new AppEngineWizardPage(false /* no library adder UI */) {
      @Override
      public void setHelp(Composite container) {
        // Do nothing in tests.
      }

      @Override
      protected MavenCoordinatesInput createMavenCoordinatesInput(Composite container) {
        mavenCoordinatesInput = new MavenCoordinatesWizardUi(container, SWT.NONE);
        return mavenCoordinatesInput;
      }
    };
    page.createControl(shellResource.getShell());
  }

  @Test
  public void testSuggestPackageName() {
    assertEquals("aa.bb", AppEngineWizardPage.suggestPackageName("aa.bb"));
    assertEquals("aA.Bb", AppEngineWizardPage.suggestPackageName("aA.Bb"));
    assertEquals("aa.bb", AppEngineWizardPage.suggestPackageName(" a  a\t . b\r b \n"));
    assertEquals("aa.bb", AppEngineWizardPage.suggestPackageName("....aa....bb..."));
    assertEquals("aa._01234bb", AppEngineWizardPage.suggestPackageName(
        "aa`~!@#$%^&*()-+=[]{}<>\\|:;'\",?/._01234bb"));
  }

  @Test
  public void testAutoPackageNameSetterOnGroupIdChange_whitespaceInGroupId() {
    Text groupIdField = getFieldWithLabel("Group ID:");
    Text javaPackageField = getFieldWithLabel("Java package:");

    groupIdField.setText(" ");  // setText() triggers VerifyEvent.
    assertEquals("", javaPackageField.getText());

    groupIdField.setText(" a");
    assertEquals("a", javaPackageField.getText());

    groupIdField.setText(" a ");
    assertEquals("a", javaPackageField.getText());

    groupIdField.setText(" a b");
    assertEquals("a", javaPackageField.getText());

    groupIdField.setText(" a ");
    assertEquals("a", javaPackageField.getText());

    groupIdField.setText(" a");
    assertEquals("a", javaPackageField.getText());

    groupIdField.setText(" ac");
    assertEquals("ac", javaPackageField.getText());
  }

  @Test
  public void testAutoPackageNameSetterOnGroupIdChange_disbledOnUserChange() {
    assertTrue(page.autoGeneratePackageName);

    Text groupIdField = getFieldWithLabel("Group ID:");
    Text javaPackageField = getFieldWithLabel("Java package:");

    groupIdField.setText("abc");
    assertEquals("abc", javaPackageField.getText());
    assertTrue(page.autoGeneratePackageName);

    javaPackageField.setText("def");
    assertFalse(page.autoGeneratePackageName);

    // javaPackageField should no longer auto-gen
    groupIdField.setText("xyz");
    assertEquals("def", javaPackageField.getText());

    // we shouldn't auto-gen even if the user clears the contents
    javaPackageField.setText("");
    assertFalse(page.autoGeneratePackageName);
    groupIdField.setText("abc");
    assertEquals("", javaPackageField.getText());
  }

  @Test
  public void testSetMavenValidationMessage_okWhenDisabled() {
    assertFalse(mavenCoordinatesInput.uiEnabled());
    assertTrue(page.setMavenValidationMessage());
  }

  @Test
  public void testSetMavenValidationMessage_emptyGroupId() {
    enableMavenCoordinatesInput();

    assertFalse(page.setMavenValidationMessage());
    assertEquals("Provide Maven Group ID.", page.getMessage());
    assertNull(page.getErrorMessage());
  }

  @Test
  public void testSetMavenValidationMessage_emptyArtifactId() {
    enableMavenCoordinatesInput();
    getFieldWithLabel("Group ID:").setText("com.example");

    assertFalse(page.setMavenValidationMessage());
    assertEquals("Provide Maven Artifact ID.", page.getMessage());
    assertNull(page.getErrorMessage());
  }

  @Test
  public void testSetMavenValidationMessage_emptyVersion() {
    enableMavenCoordinatesInput();
    getFieldWithLabel("Group ID:").setText("com.example");
    getFieldWithLabel("Artifact ID:").setText("some-artifact-id");
    getFieldWithLabel("Version:").setText("");

    assertFalse(page.setMavenValidationMessage());
    assertEquals("Provide Maven artifact version.", page.getMessage());
    assertNull(page.getErrorMessage());
  }

  @Test
  public void testSetMavenValidationMessage_illegalGroupId() {
    enableMavenCoordinatesInput();
    getFieldWithLabel("Artifact ID:").setText("some-artifact-id");

    getFieldWithLabel("Group ID:").setText("<:#= Illegal ID =#:>");
    assertFalse(page.setMavenValidationMessage());
    assertEquals("Illegal Maven Group ID: <:#= Illegal ID =#:>", page.getErrorMessage());
  }

  @Test
  public void testSetMavenValidationMessage_illegalArtifactId() {
    enableMavenCoordinatesInput();
    getFieldWithLabel("Group ID:").setText("com.example");

    getFieldWithLabel("Artifact ID:").setText("<:#= Illegal ID =#:>");
    assertFalse(page.setMavenValidationMessage());
    assertEquals("Illegal Maven Artifact ID: <:#= Illegal ID =#:>", page.getErrorMessage());
  }

  @Test
  public void testMavenValidateMavenSettings_noValidationIfUiDisabled() {
    getFieldWithLabel("Group ID:").setText("<:#= Illegal ID =#:>");
    assertTrue(page.setMavenValidationMessage());
  }

  private void enableMavenCoordinatesInput() {
    Predicate<Control> isMavenCheckbox = new Predicate<Control>() {
      @Override
      public boolean apply(Control control) {
        return control instanceof Button
            && "Create as Maven project".equals(((Button) control).getText());
      }
    };
    Button asMaven = (Button) CompositeUtil.findControl(shellResource.getShell(), isMavenCheckbox);
    new SWTBotCheckBox(asMaven).click();
  }

  private Text getFieldWithLabel(String label) {
    return CompositeUtil.findControlAfterLabel(shellResource.getShell(), Text.class, label);
  }
}
