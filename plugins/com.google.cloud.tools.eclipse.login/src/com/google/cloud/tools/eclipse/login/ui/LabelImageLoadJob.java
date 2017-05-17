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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

class LabelImageLoadJob extends Job {

  private final URL imageUrl;
  private final Label label;
  private final Display display;

  private boolean disposerAttached = false;

  @VisibleForTesting
  Image image;

  LabelImageLoadJob(URL imageUrl, Label label) {
    super("Google User Profile Picture Fetch Job");
    Preconditions.checkNotNull(imageUrl);
    this.imageUrl = imageUrl;
    this.label = label;
    display = label.getDisplay();
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    ImageDescriptor descriptor = ImageDescriptor.createFromURL(imageUrl);
    image = descriptor.createImage();
    if (image == null) {  // extreme cases; return normally.
      return Status.OK_STATUS;
    }

    try {
      LabelImageLoader.storeInCache(imageUrl.toString(), image.getImageData());
      display.syncExec(new SetImageRunnable());
    } finally {
      if (!disposerAttached) {
        image.dispose();
      }
    }
    return Status.OK_STATUS;
  }

  private class SetImageRunnable implements Runnable {

    @Override
    public void run() {
      if (!label.isDisposed()) {
        label.addDisposeListener(new ImageDisposer(image));
        disposerAttached = true;
        label.setImage(image);
      }
    }
  }
}
