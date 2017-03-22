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

package com.google.cloud.tools.eclipse.sdk.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.sdk.ui.preferences.CloudSdkPreferenceArea;
import java.io.File;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for CloudSdkPreferenceArea.
 */
@RunWith(MockitoJUnitRunner.class)
public class CloudSdkPreferenceAreaTest {
  
  @Mock
  private IPreferenceStore preferences;

  private CloudSdkPreferenceArea area;
  private Shell shell;

  @After
  public void tearDown() {
    if (shell != null && !shell.isDisposed()) {
      shell.dispose();
    }
  }

  @Test
  public void testNonExistentPath() {
    when(preferences.getString(anyString())).thenReturn(null);

    show();
    area.setStringValue("/non-existent");
    assertFalse(area.getStatus().isOK());
    assertEquals(IStatus.ERROR, area.getStatus().getSeverity());
  }

  @Test
  public void testInvalidPath() {
    when(preferences.getString(anyString())).thenReturn(null);

    File root = null;
    for (File directory : File.listRoots()) {
      if (directory.exists()) {
        // must check as roots includes A: on Windows for the floppy
        root = directory;
      }
    }
    // root should exist but not contain a valid Cloud SDK
    assertNotNull("No root directory!?", root);
    assertTrue("Root doesn't exist!?", root.exists());

    show();
    area.setStringValue(root.getAbsolutePath());
    assertEquals(IStatus.WARNING, area.getStatus().getSeverity());
    
    area.setStringValue("");
    assertEquals(IStatus.OK, area.getStatus().getSeverity());
  }

  private void show() {
    shell = new Shell(Display.getCurrent(), SWT.NONE);
    shell.setLayout(new FillLayout());

    area = new CloudSdkPreferenceArea();
    area.createContents(shell);
    area.setPreferenceStore(preferences);

    shell.open();
    runEvents();
  }

  private void runEvents() {
    while (shell != null && !shell.isDisposed() && shell.getDisplay().readAndDispatch()) {
      /* spin */
    }
  }
}
