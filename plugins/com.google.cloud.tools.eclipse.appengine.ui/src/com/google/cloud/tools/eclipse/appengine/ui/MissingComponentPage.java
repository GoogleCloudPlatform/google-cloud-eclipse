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

package com.google.cloud.tools.eclipse.appengine.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

/**
 * Wizard page that displays a message that something needed in the environment is
 * missing with instructions on how to install it. This page disables the 'Finish' button.
 */
public class MissingComponentPage extends WizardPage {

  private String message;

  /**
   * @param pageName internal identifier
   * @param title dialog title
   * @param errorMessage short error message
   * @param message detailed message and instructions. Text between single quotes is made bold.
   */
  public MissingComponentPage(String pageName, String title,
      String errorMessage, String message) {
    super(pageName);
    setTitle(title);
    setErrorMessage(errorMessage);
    this.message = message;
  }

  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);

    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint = parent.getSize().x;

    StyledText styledText = new StyledText(container, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
    styledText.setLayoutData(gridData);
    styledText.setText(message);
    styledText.setBackground(container.getBackground());
    styledText.setCaret(null /* hide caret */);

    int startIndex = message.indexOf("\'"); //$NON-NLS-1$
    int endIndex = message.indexOf("\'", startIndex + 1); //$NON-NLS-1$
    if ((-1 < startIndex) && (startIndex < endIndex)) {
      StyleRange styleRange = new StyleRange();
      styleRange.start = startIndex + 1;
      styleRange.length = endIndex - startIndex;
      styleRange.fontStyle = SWT.BOLD;
      styledText.setStyleRange(styleRange);
    }

    setControl(container);
    setPageComplete(false);
    Dialog.applyDialogFont(container);
  }

}
