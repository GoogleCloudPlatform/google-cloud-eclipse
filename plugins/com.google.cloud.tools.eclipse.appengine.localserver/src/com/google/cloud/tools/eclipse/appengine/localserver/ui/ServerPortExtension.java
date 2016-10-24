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
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.ui.wizard.ServerCreationWizardPageExtension;

/**
 * An extension that adds a server port field to the WTP server creation wizard page.
 */
public class ServerPortExtension extends ServerCreationWizardPageExtension {

  public static final String SERVER_ATTRIBUTE_PORT = "appEngineDevServerPort"; //$NON-NLS-1$
  public static final Integer DEFAULT_SERVICE_PORT = 8080;

  private static final String APP_ENGINE_SERVER_TYPE_ID =
      "com.google.cloud.tools.eclipse.appengine.standard.server"; //$NON-NLS-1$

  @VisibleForTesting Label portLabel;
  @VisibleForTesting Text portText;

  @Override
  public void createControl(UI_POSITION position, Composite parent) {
    if (position == UI_POSITION.BOTTOM) {
      portLabel = new Label(parent, SWT.NONE);
      portLabel.setVisible(false);
      portLabel.setText(Messages.NEW_SERVER_DIALOG_PORT);

      portText = new Text(parent, SWT.SINGLE | SWT.BORDER);
      portText.setVisible(false);
      portText.setText(DEFAULT_SERVICE_PORT.toString());
      portText.addVerifyListener(new NumericVerifier());
      portText.addFocusListener(new PortValueLimiter());
      portText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }
  }

  @Override
  public void handlePropertyChanged(PropertyChangeEvent event) {
    if (event != null && event.getNewValue() instanceof IServerType) {
      IServerType serverType = (IServerType) event.getNewValue();
      boolean showPort = APP_ENGINE_SERVER_TYPE_ID.equals(serverType.getId());
      portLabel.setVisible(showPort);
      portText.setVisible(showPort);
    }
  }

  private class NumericVerifier implements VerifyListener {
    @Override
    public void verifyText(VerifyEvent event) {
      String newText = portText.getText().substring(0, event.start)
          + event.text + portText.getText().substring(event.end);

      try {
        Integer.valueOf(newText);
      } catch (NumberFormatException ex) {
        event.doit = newText.isEmpty();
      }
    }
  };

  private class PortValueLimiter extends FocusAdapter {
    @Override
    public void focusLost(FocusEvent event) {
      try {
        Integer port = Integer.valueOf(portText.getText());
        port = Math.min(port, 65535);
        portText.setText(port.toString());
      } catch (NumberFormatException ex) {
        portText.setText(DEFAULT_SERVICE_PORT.toString());
      }

      serverWc.setAttribute(SERVER_ATTRIBUTE_PORT, portText.getText());
    }
  };
}
