/*******************************************************************************
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
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.cloud.tools.eclipse.appengine.localserver.Messages;
import com.google.common.annotations.VisibleForTesting;
import java.beans.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.ui.wizard.ServerCreationWizardPageExtension;

/**
 * An extension that adds a server port field to the WTP server creation wizard page.
 */
public class ServerPortExtension extends ServerCreationWizardPageExtension {

  public static final String SERVER_ATTRIBUTE_PORT = "appEngineDevServerPort"; //$NON-NLS-1$
  public static final int DEFAULT_SERVICE_PORT = 8080;

  private static final String APP_ENGINE_SERVER_TYPE_ID =
      "com.google.cloud.tools.eclipse.appengine.standard.server"; //$NON-NLS-1$

  @VisibleForTesting Label portLabel;
  @VisibleForTesting Spinner portSpinner;

  @Override
  public void createControl(UI_POSITION position, Composite parent) {
    if (position == UI_POSITION.BOTTOM) {
      portLabel = new Label(parent, SWT.NONE);
      portLabel.setVisible(false);
      portLabel.setText(Messages.NEW_SERVER_DIALOG_PORT);

      portSpinner = new Spinner(parent, SWT.BORDER);
      portSpinner.setVisible(false);
      portSpinner.setMinimum(1);
      portSpinner.setMaximum(65535);
      portSpinner.setSelection(DEFAULT_SERVICE_PORT);
      portSpinner.addModifyListener(new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent event) {
          serverWc.setAttribute(SERVER_ATTRIBUTE_PORT, portSpinner.getSelection());
        }
      });
      portSpinner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }
  }

  @Override
  public void handlePropertyChanged(PropertyChangeEvent event) {
    if (event != null && event.getNewValue() instanceof IServerType) {
      IServerType serverType = (IServerType) event.getNewValue();
      boolean showPort = APP_ENGINE_SERVER_TYPE_ID.equals(serverType.getId());
      portLabel.setVisible(showPort);
      portSpinner.setVisible(showPort);
    }
  }
}
