package com.google.cloud.tools.eclipse.ui.util;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;

public class FontUtil {

  private FontUtil() { }
  
  /**
   * Changes the font style of the control to bold.
   */
  public static void convertFontToBold(Control control) {
    FontDescriptor boldDescriptor = FontDescriptor.createFrom(control.getFont()).setStyle(SWT.BOLD);
    Font boldFont = boldDescriptor.createFont(control.getDisplay());
    control.setFont(boldFont);
  }

}
