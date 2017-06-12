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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.test.util.ui.CompositeUtil;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.common.base.Predicate;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MavenAppEngineStandardWizardPageTest {

  @Rule public ShellTestResource shellTestResource = new ShellTestResource();

  private final MavenAppEngineStandardWizardPage page =
      new MavenAppEngineStandardWizardPage(new Path("/default/workspace/location"));

  private Text locationField;
  private Button useDefaults;

  @Before
  public void setUp() {
    IWizard wizard = mock(IWizard.class);
    when(wizard.getContainer()).thenReturn(mock(IWizardContainer.class));
    page.setWizard(wizard);
    page.createControl(shellTestResource.getShell());
    locationField = findLocationField();
    useDefaults = findUseDefaultsButton();
  }

  @Test
  public void testLocationValues() {
    assertTrue(page.useDefaults());
    assertTrue(useDefaults.getSelection());
    assertFalse(locationField.getEnabled());
    assertEquals(new Path("/default/workspace/location").toOSString(), locationField.getText());
    assertEquals("", locationField.getData());

    assertEquals("/default/workspace/location", page.getLocationPath().toString());
  }

  @Test
  public void testLocationValues_uncheckUseDefaults() {
    new SWTBotCheckBox(useDefaults).click();

    assertFalse(page.useDefaults());
    assertFalse(useDefaults.getSelection());
    assertTrue(locationField.getEnabled());
    assertTrue(locationField.getText().isEmpty());
    assertEquals("", locationField.getData());

    assertEquals("", page.getLocationPath().toString());
  }

  @Test
  public void testLocationValues_uncheckAndCheckBackUseDefaults() {
    SWTBotCheckBox checkBox = new SWTBotCheckBox(useDefaults);
    checkBox.click();
    checkBox.click();

    assertTrue(page.useDefaults());
    assertTrue(useDefaults.getSelection());
    assertFalse(locationField.getEnabled());
    assertEquals(new Path("/default/workspace/location").toOSString(), locationField.getText());
    assertEquals("", locationField.getData());

    assertEquals("/default/workspace/location", page.getLocationPath().toString());
  }

  @Test
  public void testLocationValues_uncheckEnterPathAndCheckBackUseDefaults() {
    SWTBotCheckBox checkBox = new SWTBotCheckBox(useDefaults);
    checkBox.click();
    locationField.setText("/manually/entered/path");
    checkBox.click();

    assertEquals(new Path("/default/workspace/location").toOSString(), locationField.getText());
    assertEquals("/manually/entered/path", locationField.getData());

    assertEquals("/default/workspace/location", page.getLocationPath().toString());
  }

  @Test
  public void testLocationValues_uncheckEnterPathCheckBackAndUncheckUseDefaults() {
    SWTBotCheckBox checkBox = new SWTBotCheckBox(useDefaults);
    checkBox.click();
    locationField.setText("/manually/entered/path");
    checkBox.click();
    checkBox.click();

    assertEquals("/manually/entered/path", locationField.getText());
    assertEquals("/manually/entered/path", locationField.getData());

    assertEquals("/manually/entered/path", page.getLocationPath().toString());
  }

  private Text findLocationField() {
    return CompositeUtil.findControlAfterLabel((Composite) page.getControl(),
        Text.class, "Location:");
  }

  private Button findUseDefaultsButton() {
    return (Button) CompositeUtil.findControl((Composite) page.getControl(),
        new Predicate<Control>() {
          @Override
          public boolean apply(Control control) {
            return control instanceof Button
                && "Create project in workspace".equals(((Button) control).getText());
          }
        });
  }
}
