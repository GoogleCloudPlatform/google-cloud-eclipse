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

import com.google.cloud.tools.appengine.operations.cloudsdk.serialization.CloudSdkVersion;
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

/** A notification that a new version of the Cloud SDK is available. */
public class CloudSdkUpdateNotification extends AbstractNotificationPopup {
  private static final Logger logger = Logger.getLogger(CloudSdkUpdateNotification.class.getName());

  /**
   * Asynchronously shows a notification that an update is available.
   *
   * @param updateTrigger the action to take when selected; assumed to be short-lived
   */
  public static void showNotification(
      IWorkbench workbench, CloudSdkVersion currentVersion, Runnable updateTrigger) {
    workbench
        .getDisplay()
        .asyncExec(
            () -> {
              CloudSdkUpdateNotification popup =
                  new CloudSdkUpdateNotification(workbench, currentVersion, updateTrigger);
              popup.open(); // doesn't wait
            });
  }

  private IWorkbench workbench;
  private CloudSdkVersion sdkVersion;
  private Runnable updateRunnable;

  @VisibleForTesting
  CloudSdkUpdateNotification(
      IWorkbench workbench, CloudSdkVersion sdkVersion, Runnable updateRunnable) {
    super(workbench.getDisplay());
    this.workbench = workbench;
    this.sdkVersion = sdkVersion;
    this.updateRunnable = updateRunnable;
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
    message.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    message.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent event) {
            linkSelected(event.text);
          }
        });
  }

  @VisibleForTesting
  void linkSelected(String linkText) {
    if ("update".equals(linkText)) {
      updateRunnable.run();
    } else if (linkText != null && linkText.startsWith("http")) {
      IStatus status = WorkbenchUtil.openInBrowser(workbench, linkText);
      if (!status.isOK()) {
        logger.log(Level.SEVERE, status.getMessage(), status.getException());
      }
    } else {
      logger.warning("Unknown selection event: " + linkText);
    }
  }
}
