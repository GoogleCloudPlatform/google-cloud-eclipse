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

package com.google.cloud.tools.eclipse.sdk.ui;

import com.google.cloud.tools.appengine.cloudsdk.serialization.CloudSdkVersion;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import com.google.cloud.tools.eclipse.ui.util.images.SharedImages;
import java.util.logging.Logger;
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

  private static final String CLOUD_SDK_RELEASE_NOTES_URL =
      "https://cloud.google.com/sdk/docs/release-notes";

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
  private Image gcpImage;

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
    if (gcpImage == null) {
      gcpImage = SharedImages.CLOUDSDK_IMAGE_DESCRIPTOR.createImage(workbench.getDisplay());
    }
    return gcpImage;
  }

  @Override
  protected void createContentArea(Composite parent) {
    Link message = new Link(parent, SWT.WRAP);
    message.setText(Messages.getString("CloudSdkUpdateNotificationMessage", sdkVersion));
    message.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent event) {
            switch (event.text) {
              case "update":
                updateRunnable.run();
                break;
              case "releasenotes":
                showReleaseNotes();
                break;
              default:
                logger.warning("Unknown selection event: " + event.text);
                break;
            }
          }
        });
  }

  protected void showReleaseNotes() {
    WorkbenchUtil.openInBrowser(
        workbench,
        CLOUD_SDK_RELEASE_NOTES_URL,
        "com.google.cloud.tools.sdk.releasenotes",
        null,
        null);
  }

  @Override
  protected void handleShellCloseEvent() {
    super.handleShellCloseEvent();
    if (gcpImage != null && !gcpImage.isDisposed()) {
      gcpImage.dispose();
    }
    gcpImage = null;
  }
}
