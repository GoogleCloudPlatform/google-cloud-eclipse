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

import com.google.common.annotations.VisibleForTesting;

import java.util.Arrays;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

/**
 * {@link SelectionListener} that opens a {@link FileDialog} to show the file set in the associated
 * {@link Text} field (if possible) and sets the field after the user chooses a file.
 */
public class FileFieldSetter extends SelectionAdapter {

  private final Text fileField;
  private final FileDialog dialog;
  private final String[] filterExtensions;

  public FileFieldSetter(Text fileField, String[] filterExtensions) {
    this(fileField, filterExtensions, new FileDialog(fileField.getShell(), SWT.SHEET));
  }

  @VisibleForTesting
  FileFieldSetter(Text fileField, String[] filterExtensions, FileDialog fileDialog) {
    this.fileField = fileField;
    this.filterExtensions = Arrays.copyOf(filterExtensions, filterExtensions.length);
    dialog = fileDialog;
  }

  @Override
  public void widgetSelected(SelectionEvent event) {
    IPath inputPath = new Path(fileField.getText().trim());

    IPath filterPath = preProcessInputPath(inputPath);
    while (!filterPath.isEmpty() && !filterPath.isRoot() && !filterPath.toFile().isDirectory()) {
      filterPath = filterPath.removeLastSegments(1);
    }
    dialog.setFilterPath(filterPath.toString());
    dialog.setFilterExtensions(Arrays.copyOf(filterExtensions, filterExtensions.length));

    String result = dialog.open();
    if (result != null) {
      IPath pathToSet = postProcessResultPath(new Path(result));
      fileField.setText(pathToSet.toString());
    }
  }

  protected IPath preProcessInputPath(IPath inputPath) {
    return inputPath;
  }

  protected IPath postProcessResultPath(IPath result) {
    return result;
  }
}
