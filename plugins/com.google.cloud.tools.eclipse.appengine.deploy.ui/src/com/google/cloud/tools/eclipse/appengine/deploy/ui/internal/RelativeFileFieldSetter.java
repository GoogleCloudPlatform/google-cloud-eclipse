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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

/**
 * {@link SelectionListener} that opens a {@link DirectoryDialog} that is pre-configured with the
 * directory set in the associated {@link Text} field (if possible) and sets the field with the user
 * chosen directory. The path retrieved from and set to the field is relative to a given base path.
 * For example, if the base path is {@code /usr/local} and a user chose {@code /usr/local/lib/cups},
 * the text field will be set to {@code lib/cups}.
 */
public class RelativeFileFieldSetter extends SelectionAdapter {

  private final Text fileField;
  private final IPath basePath;
  private final FileDialog dialog;

  public RelativeFileFieldSetter(Text directoryField, IPath basePath) {
    this(directoryField, basePath, new FileDialog(directoryField.getShell()));
  }

  @VisibleForTesting
  RelativeFileFieldSetter(Text directoryField, IPath basePath, FileDialog directoryDialog) {
    this.fileField = directoryField;
    this.basePath = Preconditions.checkNotNull(basePath);
    Preconditions.checkArgument(basePath.isAbsolute());
    dialog = directoryDialog;
  }

  @Override
  public void widgetSelected(SelectionEvent event) {
    IPath filterPath = new Path(fileField.getText().trim());
    if (!filterPath.isAbsolute()) {
      filterPath = basePath.append(filterPath);
    }
    while (!filterPath.isRoot() && !filterPath.toFile().isDirectory()) {
      filterPath = filterPath.removeLastSegments(1);
    }
    dialog.setFilterPath(filterPath.toString());
    dialog.setFilterExtensions(new String[]{ "*.yaml" });

    String result = dialog.open();
    if (result != null) {
      IPath maybeProjectRelative = new Path(result).makeRelativeTo(basePath);
      fileField.setText(maybeProjectRelative.toString());
    }
  }
}
