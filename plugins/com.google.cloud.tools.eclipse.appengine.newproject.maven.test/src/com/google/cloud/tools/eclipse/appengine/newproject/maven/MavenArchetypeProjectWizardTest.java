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

package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MavenArchetypeProjectWizardTest {

  private MavenArchetypeProjectWizard wizard;
  private Shell shell;
  @Rule public ShellTestResource shellTestResource = new ShellTestResource();

  @Before
  public void setUp() {
    shell = shellTestResource.getShell();

    wizard = new MavenArchetypeProjectWizard();
    wizard.addPages();
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
    Assert.assertEquals("appengine-standard-archetype",
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
    Realm.runWithDefault(DisplayRealm.getRealm(Display.getDefault()), new Runnable() {
      @Override
      public void run() {
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
    });
  }

  @Test
  public void testAutoPackageNameSetterOnGroupIdChange_disbledOnUserChange() {
    Realm.runWithDefault(DisplayRealm.getRealm(Display.getDefault()), new Runnable() {
      @Override
      public void run() {
        wizard.setContainer(mock(IWizardContainer.class));
        wizard.createPageControls(shell);
        MavenAppEngineStandardWizardPage page =
            (MavenAppEngineStandardWizardPage) wizard.getPage("basicNewProjectPage");
        assertTrue(page.autoGeneratePackageName);

        page.groupIdField.setText("abc");
        assertEquals("abc", page.javaPackageField.getText());
        assertTrue(page.autoGeneratePackageName);

        page.javaPackageField.setText("def");
        assertFalse(page.autoGeneratePackageName);

        // javaPackageField should no longer auto-gen
        page.groupIdField.setText("xyz");
        assertEquals("def", page.javaPackageField.getText());

        // we shouldn't auto-gen even if the user clears the contents
        page.javaPackageField.setText("");
        assertFalse(page.autoGeneratePackageName);
        page.groupIdField.setText("abc");
        assertEquals("", page.javaPackageField.getText());
      }
    });
  }

}
