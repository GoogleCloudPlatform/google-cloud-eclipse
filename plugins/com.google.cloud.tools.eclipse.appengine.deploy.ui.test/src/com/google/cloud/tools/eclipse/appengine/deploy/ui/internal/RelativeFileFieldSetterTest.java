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

package com.google.cloud.tools.eclipse.appengine.deploy.ui.internal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RelativeFileFieldSetterTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock private Text field;
  @Mock private FileDialog dialog;
  @Mock private SelectionEvent event;

  private IPath basePath;

  @Before
  public void setUp() {
    basePath = new Path(tempFolder.getRoot().getAbsolutePath());
    assertTrue(basePath.isAbsolute());
  }

  @Test
  public void testConstructor_nonAbsoluteBasePath() {
    try {
      new RelativeFileFieldSetter(field, new Path("non/absolute/base/path"), dialog);
      fail();
    } catch (IllegalArgumentException ex) {}
  }

  @Test
  public void testFileDialogCanceled() {
    when(field.getText()).thenReturn("");
    when(dialog.open()).thenReturn(null /* means canceled */);

    new RelativeFileFieldSetter(field, basePath, dialog).widgetSelected(event);
    verify(field, never()).setText(anyString());
  }

  @Test
  public void testSetField() {
    when(field.getText()).thenReturn("");
    when(dialog.open()).thenReturn(basePath + "/sub/directory/app.yaml");

    new RelativeFileFieldSetter(field, basePath, dialog).widgetSelected(event);
    verify(field).setText("sub/directory/app.yaml");
  }

  @Test
  public void testSetField_userSuppliesPathOutsideBase() {
    when(field.getText()).thenReturn("");
    when(dialog.open()).thenReturn("/path/outside/base/app.yaml");

    new RelativeFileFieldSetter(field, new Path("/base/path"), dialog).widgetSelected(event);
    verify(field).setText("../../path/outside/base/app.yaml");
  }

  @Test
  public void testFileDialogFilterSet_relativePathInField() {
    when(field.getText()).thenReturn("src/main/appengine/app.yaml");
    when(dialog.open()).thenReturn(null);

    new RelativeFileFieldSetter(field, basePath, dialog).widgetSelected(event);
    // "basePath" is the first physically existing directory.
    verify(dialog).setFilterPath(basePath.toString());

    basePath.append("src").toFile().mkdir();
    new RelativeFileFieldSetter(field, basePath, dialog).widgetSelected(event);
    verify(dialog).setFilterPath(basePath + "/src");
  }

  @Test
  public void testFileDialogFilterSet_absolutePathInField() {
    when(field.getText()).thenReturn(basePath + "/deploy/temp/app.yaml");
    when(dialog.open()).thenReturn(null);

    new RelativeFileFieldSetter(field, basePath, dialog).widgetSelected(event);
    // "basePath" is the first physically existing directory.
    verify(dialog).setFilterPath(basePath.toString());

    basePath.append("deploy").toFile().mkdir();
    new RelativeFileFieldSetter(field, basePath, dialog).widgetSelected(event);
    verify(dialog).setFilterPath(basePath + "/deploy");
  }
}
