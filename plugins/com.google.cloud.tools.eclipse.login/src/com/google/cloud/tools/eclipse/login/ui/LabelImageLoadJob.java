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

package com.google.cloud.tools.eclipse.login.ui;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDisposer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

class LabelImageLoadJob extends Job {

  private final URL imageUrl;
  private final Label label;
  private final int width;
  private final int height;
  private final Display display;

  private boolean disposerAttached = false;

  @VisibleForTesting
  Image scaled;

  LabelImageLoadJob(URL imageUrl, Label label, int width, int height) {
    super("Google User Profile Picture Fetch Job");
    Preconditions.checkNotNull(imageUrl);
    Preconditions.checkArgument(width > 0);
    Preconditions.checkArgument(height > 0);
    this.imageUrl = imageUrl;
    this.label = label;
    this.width = width;
    this.height = height;
    display = label.getDisplay();
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    ImageDescriptor descriptor = ImageDescriptor.createFromURL(imageUrl);
    Image unscaled = descriptor.createImage();
    if (unscaled == null) {  // extreme cases; return normally.
      return Status.OK_STATUS;
    }

    try {
      ImageData scaledData = unscaled.getImageData().scaledTo(width, height);
      LabelImageLoader.storeInCache(imageUrl.toString(), scaledData);

      scaled = new Image(display, scaledData);
      display.syncExec(new SetImageRunnable());

    } finally {
      unscaled.dispose();
      if (scaled != null && !disposerAttached) {
        scaled.dispose();
      }
    }
    return Status.OK_STATUS;
  }

  private class SetImageRunnable implements Runnable {

    @Override
    public void run() {
      if (!label.isDisposed()) {
        label.addDisposeListener(new ImageDisposer(scaled));
        disposerAttached = true;
        label.setImage(scaled);
      }
    }
  }
}
