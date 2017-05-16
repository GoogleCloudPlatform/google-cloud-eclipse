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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDisposer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Label;

@VisibleForTesting
public class LabelImageLoader {

  @VisibleForTesting
  final static ConcurrentHashMap<String, ImageData> cache = new ConcurrentHashMap<>();

  @VisibleForTesting
  Job loadJob;

  /**
   * Loads an image to a {@link Label}. The image will be fetched from {@code imageUrl}
   * asynchronously if not previously cached.
   *
   * Must be called in the UI context.
   */
  void loadImage(String imageUrl, Label label) throws MalformedURLException {
    Preconditions.checkNotNull(imageUrl);

    ImageData imageData = cache.get(imageUrl);
    if (imageData != null) {
      Image image = new Image(label.getDisplay(), imageData);
      label.addDisposeListener(new ImageDisposer(image));
      label.setImage(image);
    } else {
      loadJob = new LabelImageLoadJob(new URL(imageUrl), label);
      loadJob.schedule();
    }
  }

  static void storeInCache(String imageUrl, ImageData imageData) {
    cache.put(imageUrl, imageData);
  }
}
