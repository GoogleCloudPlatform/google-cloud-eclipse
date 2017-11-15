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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.net.MalformedURLException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Label;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LabelImageLoaderTest {

  static final byte[] someImageBytes = {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72,
      68, 82, 0, 0, 0, 1, 0, 0, 0, 1, 1, 3, 0, 0, 0, 37, (byte) 219, 86, (byte) 202, 0, 0, 0, 3, 80,
      76, 84, 69, 0, 0, 0, (byte) 167, 122, 61, (byte) 218, 0, 0, 0, 1, 116, 82, 78, 83, 0, 64,
      (byte) 230, (byte) 216, 102, 0, 0, 0, 10, 73, 68, 65, 84, 8, (byte) 215, 99, 96, 0, 0, 0, 2,
      0, 1, (byte) 226, 33, (byte) 188, 51, 0, 0, 0, 0, 73, 69, 78, 68, (byte) 174, 66, 96,
      (byte) 130};
  static final ImageData someImageData = new ImageData(
      1, 1, 1, new PaletteData(new RGB[] { new RGB(0, 0, 0) }));

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  private final LabelImageLoader imageLoader = new LabelImageLoader();
  private Label label;

  @Before
  public void setUp() {
    label = new Label(shellResource.getShell(), SWT.NONE);
  }

  @After
  public void tearDown() {
    Image image = label.getImage();
    label.dispose();
    if (image != null) {
      assertTrue("FIX BUG: DisposeListener didn't run?", image.isDisposed());
    }

    LabelImageLoader.cache.clear();
  }

  @Test
  public void testLoadImage_nullImageUrl() throws MalformedURLException {
    try {
      imageLoader.loadImage(null, label);
      fail();
    } catch (NullPointerException ex) {}
  }

  @Test
  public void testLoadImage_malformedImageUrl() {
    try {
      imageLoader.loadImage("malformed", label);
      fail();
    } catch (MalformedURLException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void testStoreInCache() {
    assertTrue(LabelImageLoader.cache.isEmpty());
    LabelImageLoader.storeInCache("http://example.com", someImageData);
    assertEquals(1, LabelImageLoader.cache.size());
    assertSame(someImageData, LabelImageLoader.cache.get("http://example.com"));
  }

  @Test
  public void testLoadImage_notAsyncIfCached() throws MalformedURLException {
    LabelImageLoader.storeInCache("http://example.com", someImageData);

    imageLoader.loadImage("http://example.com", label);
    assertNull(imageLoader.loadJob);
    assertNotNull(label.getImage());
  }
}
