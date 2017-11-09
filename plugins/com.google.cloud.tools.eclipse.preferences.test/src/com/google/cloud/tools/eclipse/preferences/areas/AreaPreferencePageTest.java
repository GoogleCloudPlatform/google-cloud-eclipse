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

package com.google.cloud.tools.eclipse.preferences.areas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AreaPreferencePageTest {
  private @Mock IPersistentPreferenceStore preferences;

  private AreaBasedPreferencePage page;
  private TestPrefArea area1;
  private TestPrefArea area2;
  private TestPrefArea area3;

  private Shell shell;

  @After
  public void tearDown() {
    if (shell != null && !shell.isDisposed()) {
      shell.dispose();
    }
  }

  @Test
  public void testWritesOnApply() {
    page = new AreaBasedPreferencePage("test");
    page.addArea(new TestPrefArea("pref1", "value", preferences));
    show(page);
    assertTrue(page.performOk());
    verify(preferences).setValue("pref1", "value");
  }

  @Test
  public void testNoWritesOnCancel() {
    page = new AreaBasedPreferencePage("test");
    page.addArea(new TestPrefArea("pref1", "value", preferences));
    show(page);

    page.performCancel();
    verify(preferences, never()).setValue(anyString(), anyString());
  }

  @Test
  public void testErrorStatusResults() {
    page = new AreaBasedPreferencePage("test");
    area1 = new TestPrefArea("pref1", "value", preferences);
    page.addArea(area1);

    show(page);
    assertTrue(page.isValid());
    assertEquals(IMessageProvider.NONE, page.getMessageType());

    area1.status = new Status(IStatus.ERROR, "foo", "message");
    area1.fireValueChanged(TestPrefArea.IS_VALID, true, false);

    assertFalse(page.isValid());
    assertEquals(IMessageProvider.ERROR, page.getMessageType());
    assertEquals("message", page.getMessage());
  }

  @Test
  public void testStatusTransition() {
    testErrorStatusResults();
    // change its status to be OK, trigger its VALUE property.
    // verify no error message
    area1.status = Status.OK_STATUS;
    area1.fireValueChanged(TestPrefArea.IS_VALID, false, true);

    assertTrue(page.isValid());
    assertEquals(IMessageProvider.NONE, page.getMessageType());
  }

  @Test
  public void testStatusSeverityInfo() {
    // add three areas, status = OK, OK, INFO; verify showing INFO
    setupAreas();

    area3.status = new Status(IStatus.INFO, "foo", "info3");
    area3.fireValueChanged(TestPrefArea.IS_VALID, true, false);

    assertTrue("should still be valid", page.isValid());
    assertEquals(IMessageProvider.INFORMATION, page.getMessageType());
    assertEquals("info3", page.getMessage());
  }

  @Test
  public void testStatusSeverityWarn() {
    // add three areas, status = OK, WARN, INFO; verify showing WARN
    setupAreas();

    area3.status = new Status(IStatus.INFO, "foo", "info3");
    area3.fireValueChanged(TestPrefArea.IS_VALID, true, false);

    assertTrue("should still be valid", page.isValid());
    assertEquals(IMessageProvider.INFORMATION, page.getMessageType());
    assertEquals("info3", page.getMessage());

    area2.status = new Status(IStatus.WARNING, "foo", "warn2");
    area2.fireValueChanged(TestPrefArea.IS_VALID, true, false);

    assertTrue("should still be valid", page.isValid());
    assertEquals(IMessageProvider.WARNING, page.getMessageType());
    assertEquals("warn2", page.getMessage());
  }

  @Test
  public void testStatusSeverityError() {
    // add three areas, status = WARN, ERROR, INFO; verify showing ERROR

    setupAreas();

    area3.status = new Status(IStatus.INFO, "foo", "info3");
    area3.fireValueChanged(TestPrefArea.IS_VALID, true, false);

    assertTrue("should still be valid", page.isValid());
    assertEquals(IMessageProvider.INFORMATION, page.getMessageType());
    assertEquals("info3", page.getMessage());

    area1.status = new Status(IStatus.WARNING, "foo", "warn1");
    area1.fireValueChanged(TestPrefArea.IS_VALID, true, false);

    assertTrue("should still be valid", page.isValid());
    assertEquals(IMessageProvider.WARNING, page.getMessageType());
    assertEquals("warn1", page.getMessage());

    area2.status = new Status(IStatus.ERROR, "foo", "error2");
    area2.fireValueChanged(TestPrefArea.IS_VALID, true, false);

    assertFalse("should now be invalid", page.isValid());
    assertEquals(IMessageProvider.ERROR, page.getMessageType());
    assertEquals("error2", page.getMessage());
  }

  private void setupAreas() {
    page = new AreaBasedPreferencePage("test");
    area1 = new TestPrefArea("pref1", "value", preferences);
    area2 = new TestPrefArea("pref2", "value", preferences);
    area3 = new TestPrefArea("pref3", "value", preferences);
    page.addArea(area1);
    page.addArea(area2);
    page.addArea(area3);

    area1.status = Status.OK_STATUS;
    area2.status = Status.OK_STATUS;
    area3.status = Status.OK_STATUS;

    show(page);
    assertTrue(page.isValid());
    assertEquals(IMessageProvider.NONE, page.getMessageType());
  }

  private void show(AreaBasedPreferencePage page) {
    shell = new Shell(Display.getCurrent());
    Composite composite = new Composite(shell, SWT.NONE);
    page.createControl(composite);
  }

  /**
   * Test preference area that reads and writes a string from preferences.
   */
  private static class TestPrefArea extends PreferenceArea {
    IStatus status = Status.OK_STATUS;
    String preferenceName;
    String preferenceValue;

    public TestPrefArea(String preferenceName, String preferenceValue,
        IPersistentPreferenceStore preferences) {
      this.preferenceName = preferenceName;
      this.preferenceValue = preferenceValue;
      setPreferenceStore(preferences);
    }

    @Override
    public Control createContents(Composite parent) {
      return parent;
    }

    @Override
    public IStatus getStatus() {
      return status;
    }

    @Override
    public void load() {
      getPreferenceStore().getString(preferenceName);
    }

    @Override
    public void loadDefault() {
      getPreferenceStore().getDefaultString(preferenceName);
    }

    @Override
    public void performApply() {
      getPreferenceStore().setValue(preferenceName, preferenceValue);
    }
  }
}
