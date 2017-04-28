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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.Path;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Text;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RelativeDirectoryFieldSetterTest {

  @Mock private Text field;
  @Mock private DirectoryDialog dialog;
  @Mock private SelectionEvent event;

  @Test
  public void testConstructor_nonAbsoluteBasePath() {
    try {
      new RelativeDirectoryFieldSetter(field, new Path("non/absolute/path"), dialog);
      fail();
    } catch (IllegalArgumentException ex) {}
  }

  @Test
  public void testDirectoryDialogCanceled() {
    when(field.getText()).thenReturn("");
    when(dialog.open()).thenReturn(null /* means canceled */);

    new RelativeDirectoryFieldSetter(field, new Path("/base/path"), dialog).widgetSelected(event);
    verify(field, never()).setText("sub/directory");
  }

  @Test
  public void testSetField() {
    when(field.getText()).thenReturn("");
    when(dialog.open()).thenReturn("/base/path/sub/directory");

    new RelativeDirectoryFieldSetter(field, new Path("/base/path"), dialog).widgetSelected(event);
    verify(field).setText("sub/directory");
  }

  @Test
  public void testSetField_userSuppliesPathOutsideBase() {
    when(field.getText()).thenReturn("");
    when(dialog.open()).thenReturn("/path/outside/base");

    new RelativeDirectoryFieldSetter(field, new Path("/base/path"), dialog).widgetSelected(event);
    verify(field).setText("../../path/outside/base");
  }

  @Test
  public void testDirectoryDialogFilterSet_relativePathInField() {
    when(field.getText()).thenReturn("src/main/appengine");
    when(dialog.open()).thenReturn(null);

    new RelativeDirectoryFieldSetter(field, new Path("/base/path"), dialog).widgetSelected(event);
    verify(dialog).setFilterPath("/base/path/src/main/appengine");
  }

  @Test
  public void testDirectoryDialogFilterSet_absoluatePathInField() {
    when(field.getText()).thenReturn("/usr/local/bin");
    when(dialog.open()).thenReturn(null);

    new RelativeDirectoryFieldSetter(field, new Path("/base/path"), dialog).widgetSelected(event);
    verify(dialog).setFilterPath("/usr/local/bin");
  }
}
