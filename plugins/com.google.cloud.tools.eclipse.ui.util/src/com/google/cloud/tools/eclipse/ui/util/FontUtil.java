/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.ui.util;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Control;

public class FontUtil {

  private FontUtil() { }
  
  /**
   * Changes the font style of the control to bold unless it already is bold.
   * <p>
   * If the font is converted to bold, it will attach a {@link DisposeListener}
   * to the <code>control</code> to dispose the font when it's not needed anymore.
   * <p>
   * <em>If converting fonts to bold is a frequent operation, this method will create
   * several {@link DisposeListener}s that can lead to high resource allocation</em>
   */
  public static void convertFontToBold(Control control) {
    convertFont(control, SWT.BOLD);
  }

  /**
   * Changes the font style of the control to italic unless it already is italic.
   * <p>
   * If the font is converted to italic, it will attach a {@link DisposeListener}
   * to the <code>control</code> to dispose the font when it's not needed anymore.
   * <p>
   * <em>If converting fonts to italic is a frequent operation, this method will create
   * several {@link DisposeListener}s that can lead to high resource allocation</em>
   */
  public static void convertFontToItalic(Control control) {
    convertFont(control, SWT.ITALIC);
  }

  /**
   * Converts the font of the control by adding a single style bit, unless the font already have
   * that style.
   * <p>
   * If the font is converted, it will attach a {@link DisposeListener}
   * to the <code>control</code> to dispose the font when it's not needed anymore.
   * <p>
   * <em>If converting fonts is a frequent operation, this method will create
   * several {@link DisposeListener}s that can lead to high resource allocation</em>
   *
   * @param control whose font will be changed
   * @param style e.g. SWT.BOLD or SWT.ITALIC
   */
  public static void convertFont(Control control, int style) {
    for (FontData fontData : control.getFont().getFontData()) {
      if (hasStyle(fontData, style)) {
        return;
      }
    }
    FontDescriptor fontDescriptor = FontDescriptor.createFrom(control.getFont()).setStyle(style);
    final Font newFont = fontDescriptor.createFont(control.getDisplay());
    control.setFont(newFont);
    control.addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent event) {
        newFont.dispose();
      }
    });
  }

  private static boolean hasStyle(FontData fontData, int style) {
    return (fontData.getStyle() & style) == style;
  }
}
