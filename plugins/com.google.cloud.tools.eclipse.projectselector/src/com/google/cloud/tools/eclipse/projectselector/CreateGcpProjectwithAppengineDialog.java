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

package com.google.cloud.tools.eclipse.projectselector;

import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.ErrorDialogErrorHandler;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

public class CreateGcpProjectwithAppengineDialog extends Dialog {

  public CreateGcpProjectwithAppengineDialog(Shell parentShell) {
    super(parentShell);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    getShell().setText(Messages.getString("dialog.createproject.title"));
    Composite control = (Composite) super.createDialogArea(parent);
    Link link = new Link(control, SWT.NONE);
    link.setText("You can create a new GCP project in the <a href=\"https://console.cloud.google.com/projectselector/appengine/create?lang=java\">Cloud Console</a>");
    link.addSelectionListener(new OpenUriSelectionListener(new ErrorDialogErrorHandler(getShell())));
    return control;
  }
}