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

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.cloud.tools.eclipse.appengine.localserver.Messages;
import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour;
import com.google.common.annotations.VisibleForTesting;
import java.beans.PropertyChangeEvent;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
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

  public static final int DEFAULT_SERVICE_PORT = 8080;

  private static final String APP_ENGINE_SERVER_TYPE_ID =
      "com.google.cloud.tools.eclipse.appengine.standard.server"; //$NON-NLS-1$

  @VisibleForTesting Label portLabel;
  @VisibleForTesting Text portText;
  @VisibleForTesting ControlDecoration portDecoration;

  @VisibleForTesting Image informationImage;
  @VisibleForTesting Image errorImage;

  @Override
  public void createControl(UI_POSITION position, Composite parent) {
    if (position == UI_POSITION.BOTTOM) {
      portLabel = new Label(parent, SWT.NONE);
      portLabel.setVisible(false);
      portLabel.setText(Messages.NEW_SERVER_DIALOG_PORT);

      portText = new Text(parent, SWT.SINGLE | SWT.BORDER);
      portText.setVisible(false);
      portText.setText(String.valueOf(DEFAULT_SERVICE_PORT));
      portText.addVerifyListener(new PortChangeMonitor());
      portText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      portDecoration = new ControlDecoration(portText, SWT.LEFT | SWT.TOP);
      portDecoration.hide();

      FieldDecorationRegistry registry = FieldDecorationRegistry.getDefault();
      informationImage =
          registry.getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage();
      errorImage = registry.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage();
    }
  }

  @Override
  public void handlePropertyChanged(PropertyChangeEvent event) {
    if (event != null && event.getNewValue() instanceof IServerType) {
      IServerType serverType = (IServerType) event.getNewValue();
      boolean showPort = APP_ENGINE_SERVER_TYPE_ID.equals(serverType.getId());
      portLabel.setVisible(showPort);
      portText.setVisible(showPort);
      if (showPort) {
        updatePortAndTriggerDecoration(portText.getText());
      } else {
        portDecoration.hide();
      }
    }
  }

  private class PortChangeMonitor implements VerifyListener {
    @Override
    public void verifyText(VerifyEvent event) {
      String newText = portText.getText().substring(0, event.start)
          + event.text + portText.getText().substring(event.end);
      event.doit = updatePortAndTriggerDecoration(newText);
    }
  };

  private boolean updatePortAndTriggerDecoration(String newPortString) {
    if (newPortString.isEmpty()) {
      showPortDecoration(informationImage, Messages.NEW_SERVER_DIALOG_EMPTY_PORT_FIELD);
      serverWc.setAttribute(LocalAppEngineServerBehaviour.SERVER_ATTRIBUTE_PORT, 0);
      return true;
    }

    try {
      int port = Integer.parseInt(newPortString);
      serverWc.setAttribute(LocalAppEngineServerBehaviour.SERVER_ATTRIBUTE_PORT, port);

      if (port <= 65535) {
        portDecoration.hide();
      } else {
        showPortDecoration(errorImage, Messages.NEW_SERVER_DIALOG_INVALID_PORT_VALUE);
      }
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  private void showPortDecoration(Image image, String description) {
    portDecoration.setImage(image);
    portDecoration.setDescriptionText(description);
    portDecoration.show();
    portDecoration.showHoverText(description);
  }
}
