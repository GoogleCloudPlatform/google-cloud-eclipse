/*
 * Copyright 2018 Google Inc.
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

package com.google.cloud.tools.eclipse.ui.status;

import com.google.cloud.tools.eclipse.ui.util.ServiceUtils;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ShowGcpStatusHandler extends AbstractHandler implements IElementUpdater {
  private static final Logger logger = Logger.getLogger(ShowGcpStatusHandler.class.getName());

  private static final String STATUS_URL = "https://status.cloud.google.com";

  private static final String BUNDLE_ID = "com.google.cloud.tools.eclipse.ui.status";

  private static final ImageDescriptor IMG_OK =
      AbstractUIPlugin.imageDescriptorFromPlugin(BUNDLE_ID, "icons/gcp-ok.png");
  private static final ImageDescriptor IMG_LOW =
      AbstractUIPlugin.imageDescriptorFromPlugin(BUNDLE_ID, "icons/gcp-low.png");
  private static final ImageDescriptor IMG_MEDIUM =
      AbstractUIPlugin.imageDescriptorFromPlugin(BUNDLE_ID, "icons/gcp-medium.png");
  private static final ImageDescriptor IMG_HIGH =
      AbstractUIPlugin.imageDescriptorFromPlugin(BUNDLE_ID, "icons/gcp-high.png");
  private static final ImageDescriptor IMG_ERROR =
      AbstractUIPlugin.imageDescriptorFromPlugin(
          "org.eclipse.jface", "icons/full/message_error.png");

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    GcpStatusMonitoringService service = ServiceUtils.getService(event, GcpStatusMonitoringService.class);
    ((PollingStatusServiceImpl) service).refreshStatus();
    WorkbenchUtil.openInBrowser(PlatformUI.getWorkbench(), STATUS_URL);
    return null;
  }

  @Override
  public void updateElement(UIElement element, Map parameters) {
    GcpStatusMonitoringService service = element.getServiceLocator().getService(GcpStatusMonitoringService.class);
    GcpStatus status = service.getCurrentStatus();
    element.setText("Status: " + status.summary);
    switch (status.severity) {
      case OK:
        element.setIcon(IMG_OK);
        element.setTooltip(status.summary);
        break;
      case LOW:
        element.setIcon(IMG_LOW);
        element.setTooltip(summarizeIncidents(status.active));
        break;
      case MEDIUM:
        element.setIcon(IMG_MEDIUM);
        element.setTooltip(summarizeIncidents(status.active));
        break;
      case HIGH:
        element.setIcon(IMG_HIGH);
        element.setTooltip(summarizeIncidents(status.active));
        break;
      case ERROR:
      default:
        element.setIcon(IMG_ERROR);
        element.setTooltip(status.summary); // show error text
        break;
    }
  }

  private String summarizeIncidents(Collection<Incident> incidents) {
    List<String> messages = new ArrayList<>();
    for (Incident incident : incidents) {
      messages.add(incident.toString());
    }
    return Joiner.on("\n").join(messages);
  }
}
