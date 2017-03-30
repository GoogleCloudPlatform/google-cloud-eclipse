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

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

class ImageFetcher {

  public static void setImageLazily(String imageUrl, final Label placeholder)
      throws MalformedURLException {
    ImageDescriptor descriptor = ImageDescriptor.createFromURL(new URL(imageUrl));
    Display display = placeholder.getDisplay();

    display.syncExec(new Runnable() {
      @Override
      public void run() {
        if (placeholder.isDisposed()) {
        }
      }
    });
  }

}
