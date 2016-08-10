package com.google.cloud.tools.eclipse.ui.util;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.ExpandBar;

public class FontUtil {

  private FontUtil() { }
  
  public static void convertFontToBold(ExpandBar expandBar) {
    FontDescriptor boldDescriptor = FontDescriptor.createFrom(expandBar.getFont()).setStyle(SWT.BOLD);
    Font boldFont = boldDescriptor.createFont(expandBar.getDisplay());
    expandBar.setFont(boldFont);
  }

}
