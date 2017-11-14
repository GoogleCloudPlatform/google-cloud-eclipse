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
import com.google.common.base.Preconditions;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

/**
 * {@link SelectionListener} that opens a {@link FileDialog} to show the file set in the associated
 * {@link Text} field (if possible) and sets the field after the user chooses a file. The path
 * retrieved from and set to the field is relative to a given base path. For example, if the base
 * path is {@code /usr/local} and a user chose {@code /usr/local/lib/cups}, the text field will be
 * set to {@code lib/cups}.
 */
public class RelativeFileFieldSetter extends FileFieldSetter {

  private final IPath basePath;

  public RelativeFileFieldSetter(Text fileField, IPath basePath, String[] filterExtensions) {
    this(fileField, basePath, filterExtensions, new FileDialog(fileField.getShell(), SWT.SHEET));
  }

  @VisibleForTesting
  RelativeFileFieldSetter(Text fileField, IPath basePath, String[] filterExtensions,
      FileDialog fileDialog) {
    super(fileField, filterExtensions, fileDialog);
    this.basePath = Preconditions.checkNotNull(basePath);
    Preconditions.checkArgument(basePath.isAbsolute(), "basePath not absolute");
  }

  @Override
  protected IPath preProcessInputPath(IPath inputPath) {
    if (!inputPath.isAbsolute()) {
      inputPath = basePath.append(inputPath);
    }
    return inputPath;
  }

  @Override
  protected IPath postProcessResultPath(IPath resultPath) {
    return resultPath.makeRelativeTo(basePath);
  }
}
