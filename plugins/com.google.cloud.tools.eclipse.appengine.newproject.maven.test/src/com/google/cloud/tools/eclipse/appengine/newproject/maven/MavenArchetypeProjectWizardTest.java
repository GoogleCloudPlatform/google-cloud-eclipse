/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import static org.mockito.Mockito.mock;

import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MavenArchetypeProjectWizardTest {

  private MavenArchetypeProjectWizard wizard;
  private Shell shell;

  @Before
  public void setUp() {
    // TODO(chanseok): use ShellTestResource (see AccountPanelTest) after fixing
    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/771.
    // (Remove shell.dispose() in tearDown() too.)
    assertNotNull(Display.getDefault());
    shell = new Shell(Display.getDefault());

    wizard = new MavenArchetypeProjectWizard();
    wizard.addPages();
  }

  @After
  public void tearDown() {
    shell.dispose();
  }

  @Test
  public void testCanFinish() {
    Assert.assertFalse(wizard.canFinish());
  }

  @Test
  public void testTwoPages() {
    Assert.assertEquals(2, wizard.getPageCount());
  }

  @Test
  public void testGetPageByName() {
    assertNotNull(wizard.getPage("basicNewProjectPage"));
    assertNotNull(wizard.getPage("newProjectArchetypePage"));
  }

  @Test
  public void testPageComplete() {
    Assert.assertFalse(wizard.getPage("newProjectArchetypePage").isPageComplete());
    Assert.assertFalse(wizard.getPage("newProjectArchetypePage").isPageComplete());
  }

  @Test
  public void testFlipToPageTwo() {
    Assert.assertFalse(wizard.getPage("basicNewProjectPage").canFlipToNextPage());
  }

  @Test
  public void testPageOrder() {
    Assert.assertEquals(wizard.getPage("newProjectArchetypePage"),
        wizard.getPage("basicNewProjectPage").getNextPage());
    Assert.assertEquals(wizard.getPage("basicNewProjectPage"),
        wizard.getPage("newProjectArchetypePage").getPreviousPage());
  }

  @Test
  public void testArchetypeDefaultSelection() {
    Assert.assertEquals("appengine-skeleton-archetype",
        MavenAppEngineStandardArchetypeWizardPage.PRESET_ARCHETYPES.get(0)
            .archetype.getArtifactId());
  }

  @Test
  public void testSuggestPackageName() {
    assertEquals("aa.bb", MavenAppEngineStandardWizardPage.suggestPackageName("aa.bb"));

    assertEquals("aA.Bb", MavenAppEngineStandardWizardPage.suggestPackageName("aA.Bb"));

    assertEquals("aa.bb",
        MavenAppEngineStandardWizardPage.suggestPackageName(" a  a\t . b\r b \n"));

    assertEquals("aa.bb",
        MavenAppEngineStandardWizardPage.suggestPackageName("....aa....bb..."));

    assertEquals("aa._01234bb", MavenAppEngineStandardWizardPage.suggestPackageName(
        "aa`~!@#$%^&*()-+=[]{}<>\\|:;'\",?/._01234bb"));
  }

  @Test
  public void testAutoPackageNameSetterOnGroupIdChange_whitespaceInGroupId() {
    wizard.setContainer(mock(IWizardContainer.class));
    wizard.createPageControls(shell);
    MavenAppEngineStandardWizardPage page =
        (MavenAppEngineStandardWizardPage) wizard.getPage("basicNewProjectPage");

    page.groupIdField.setText(" ");  // setText() triggers VerifyEvent.
    assertEquals("", page.javaPackageField.getText());

    page.groupIdField.setText(" a");
    assertEquals("a", page.javaPackageField.getText());

    page.groupIdField.setText(" a ");
    assertEquals("a", page.javaPackageField.getText());

    page.groupIdField.setText(" a b");
    assertEquals("a", page.javaPackageField.getText());

    page.groupIdField.setText(" a ");
    assertEquals("a", page.javaPackageField.getText());

    page.groupIdField.setText(" a");
    assertEquals("a", page.javaPackageField.getText());

    page.groupIdField.setText(" ac");
    assertEquals("ac", page.javaPackageField.getText());
  }
}
