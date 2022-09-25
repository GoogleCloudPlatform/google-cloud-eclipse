/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.ui.privacytos;

import com.google.cloud.tools.eclipse.preferences.areas.PreferenceArea;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.ErrorDialogErrorHandler;
import com.google.cloud.tools.eclipse.ui.util.Messages;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;

/**
 * Adds a section to main CTE preferences stating Google TOS
 */
public class TosPreferenceArea extends PreferenceArea {

  @Override
  public Control createContents(Composite container) {
    String link = Messages.getString("privacytos.settings.disclaimer");
    Link newLink = new Link(container, SWT.WRAP);
    newLink.setText(link);
    newLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.GRAB_HORIZONTAL));
    newLink.addSelectionListener(new OpenUriSelectionListener(new ErrorDialogErrorHandler(container.getShell())));
    return newLink;
  }

  @Override
  public IStatus getStatus() {
    return Status.OK_STATUS;
  }

  @Override
  public void load() {
    // This preference Area only displays a message - no need to load anything
  }

  @Override
  public void loadDefault() {
    // This preference Area only displays a message - no need to load anything
  }

  @Override
  public void performApply() {
    // This preference Area only displays a message - no need to load anything
    
  }

}
