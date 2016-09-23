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
    for (FontData fontData : control.getFont().getFontData()) {
      if ((fontData.getStyle() & SWT.BOLD) != 0) {
        return;
      }
    }
    FontDescriptor boldDescriptor = FontDescriptor.createFrom(control.getFont()).setStyle(SWT.BOLD);
    final Font boldFont = boldDescriptor.createFont(control.getDisplay());
    control.setFont(boldFont);
    control.addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent event) {
        boldFont.dispose();
      }
    });
  }

}
