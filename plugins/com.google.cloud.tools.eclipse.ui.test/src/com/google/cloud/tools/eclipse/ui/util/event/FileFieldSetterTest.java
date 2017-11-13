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

package com.google.cloud.tools.eclipse.ui.util.event;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FileFieldSetterTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock private Text field;
  @Mock private FileDialog dialog;
  @Mock private SelectionEvent event;
  private final String[] filter = new String[0];

  @Test
  public void testFileDialogCanceled() {
    when(field.getText()).thenReturn("");
    when(dialog.open()).thenReturn(null /* means canceled */);

    new FileFieldSetter(field, filter, dialog).widgetSelected(event);
    verify(field, never()).setText(anyString());
  }

  @Test
  public void testSetField() {
    when(field.getText()).thenReturn("");
    when(dialog.open()).thenReturn("/absolute/path/to/app.yaml");

    new FileFieldSetter(field, filter, dialog).widgetSelected(event);
    verify(field).setText("/absolute/path/to/app.yaml");
  }

  @Test
  public void testFileDialogFilterSet_relativePathInField() {
    when(field.getText()).thenReturn("relative/path/file.txt");
    when(dialog.open()).thenReturn(null);

    new FileFieldSetter(field, filter, dialog).widgetSelected(event);
    verify(dialog).setFilterPath("");  // Empty means the FileDialog opens the "default" directory.
  }

  @Test
  public void testFileDialogFilterSet_absolutePathInField() {
    IPath absolutePath = new Path(tempFolder.getRoot().getAbsolutePath());
    when(field.getText()).thenReturn(absolutePath + "/sub/directory/file.txt");
    when(dialog.open()).thenReturn(null);

    new FileFieldSetter(field, filter, dialog).widgetSelected(event);
    // "absolutePath" is the first physically existing directory.
    verify(dialog).setFilterPath(absolutePath.toString());

    absolutePath.append("sub").toFile().mkdir();
    new FileFieldSetter(field, filter, dialog).widgetSelected(event);
    verify(dialog).setFilterPath(absolutePath + "/sub");
  }

  @Test
  public void testFileDialogFileterExtensionSet() {
    when(field.getText()).thenReturn("");
    new FileFieldSetter(field, filter, dialog).widgetSelected(event);
    verify(dialog).setFilterExtensions(filter);
  }
}
