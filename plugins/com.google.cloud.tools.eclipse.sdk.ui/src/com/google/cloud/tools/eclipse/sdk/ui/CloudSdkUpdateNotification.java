/*
 * Copyright 2018 Google LLC
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

package com.google.cloud.tools.eclipse.sdk.ui;

import com.google.cloud.tools.appengine.cloudsdk.serialization.CloudSdkVersion;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;

/** A notification that a new version of the Cloud SDK is available. */
public class CloudSdkUpdateNotification extends AbstractNotificationPopup {
  private static final Logger logger = Logger.getLogger(CloudSdkUpdateNotification.class.getName());

  /** Show a notification that an update is available. */
  public static void showNotification(
      IWorkbench workbench, CloudSdkVersion currentVersion, Runnable updateTrigger) {
    workbench
        .getDisplay()
        .asyncExec(
            () -> {
              CloudSdkUpdateNotification popup =
                  new CloudSdkUpdateNotification(workbench, currentVersion, updateTrigger);
              popup.open();
            });
  }

  private IWorkbench workbench;
  private CloudSdkVersion sdkVersion;
  private Runnable updateRunnable;

  private CloudSdkUpdateNotification(
      IWorkbench wb, CloudSdkVersion currentVersion, Runnable triggerUpdate) {
    super(wb.getDisplay());
    workbench = wb;
    sdkVersion = currentVersion;
    updateRunnable = triggerUpdate;
  }

  @Override
  protected String getPopupShellTitle() {
    return Messages.getString("CloudSdkUpdateNotificationTitle");
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return resources.createImage(SharedImages.CLOUDSDK_IMAGE_DESCRIPTOR);
  }

  @Override
  protected void createContentArea(Composite parent) {
    Link message = new Link(parent, SWT.WRAP);
    message.setText(Messages.getString("CloudSdkUpdateNotificationMessage", sdkVersion));
    message.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent event) {
            if ("update".equals(event.text)) {
              updateRunnable.run();
            } else if (event.text != null && event.text.startsWith("http")) {
              IStatus status = WorkbenchUtil.openInBrowser(workbench, event.text);
              if (!status.isOK()) {
                logger.log(Level.SEVERE, status.getMessage(), status.getException());
              }
            } else {
              logger.warning("Unknown selection event: " + event.text);
            }
          }
        });
  }
}
