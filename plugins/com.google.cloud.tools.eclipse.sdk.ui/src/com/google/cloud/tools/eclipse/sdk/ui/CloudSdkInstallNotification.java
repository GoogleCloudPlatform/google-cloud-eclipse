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

import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import com.google.common.annotations.VisibleForTesting;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;

/**
 * Notifies the user that the Cloud SDK will be installed. The installation will proceed unless the
 * user explicitly cancels the update.
 */
public class CloudSdkInstallNotification extends AbstractNotificationPopup {
  private static final Logger logger =
      Logger.getLogger(CloudSdkInstallNotification.class.getName());

  /**
   * Asynchronously shows a notification that installation is about to proceed.
   *
   * @param installTrigger the action to take when selected; assumed to be short-lived
   */
  public static void showNotification(IWorkbench workbench, Runnable installTrigger) {
    workbench
        .getDisplay()
        .asyncExec(
            () -> {
              CloudSdkInstallNotification popup =
                  new CloudSdkInstallNotification(workbench, installTrigger);
              popup.open(); // doesn't wait
            });
  }

  @VisibleForTesting boolean shouldInstall = true;
  private IWorkbench workbench;
  private Runnable installRunnable;

  @VisibleForTesting
  CloudSdkInstallNotification(IWorkbench workbench, Runnable installRunnable) {
    super(workbench.getDisplay());
    this.workbench = workbench;
    this.installRunnable = installRunnable;
  }

  @Override
  protected String getPopupShellTitle() {
    return Messages.getString("CloudSdkInstallNotificationTitle");
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return resources.createImage(SharedImages.CLOUDSDK_IMAGE_DESCRIPTOR);
  }

  @Override
  protected void createContentArea(Composite parent) {
    Link message = new Link(parent, SWT.WRAP);
    message.setText(Messages.getString("CloudSdkInstallNotificationMessage"));
    message.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    message.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent event) {
            linkSelected(event.text);
          }
        });
  }

  /** React to the user selecting a link within the notification. */
  @VisibleForTesting
  void linkSelected(String linkText) {
    if ("skip".equals(linkText)) {
      shouldInstall = false;
      close();
    } else if (linkText != null && linkText.startsWith("http")) {
      IStatus status = WorkbenchUtil.openInBrowser(workbench, linkText);
      if (!status.isOK()) {
        logger.log(Level.SEVERE, status.getMessage(), status.getException());
      }
    } else {
      logger.warning("Unknown selection event: " + linkText);
    }
  }

  @Override
  public boolean close() {
    if (shouldInstall) {
      installRunnable.run();
    }
    return super.close();
  }
}
