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
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineWizardPageTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  private AppEngineWizardPage page;

  @Before
  public void setUp() {
    page = new AppEngineWizardPage() {
      @Override
      public void setHelp(Composite container) {
        // Do nothing in tests.
      }

      @Override
      protected String getSupportedLibrariesGroup() {
        return "";
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
    Text groupIdField = getFieldWithLabel(page, "Group ID:");
    Text javaPackageField = getFieldWithLabel(page, "Java package:");

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

    Text groupIdField = getFieldWithLabel(page, "Group ID:");
    Text javaPackageField = getFieldWithLabel(page, "Java package:");

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

  private static Text getFieldWithLabel(WizardPage page, String label) {
    return CompositeUtil.findControlAfterLabel((Composite) page.getControl(), Text.class, label);
  }

}
