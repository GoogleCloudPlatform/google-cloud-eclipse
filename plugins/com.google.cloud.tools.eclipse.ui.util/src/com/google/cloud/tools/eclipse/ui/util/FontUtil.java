package com.google.cloud.tools.eclipse.ui.util;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.Control;

public class FontUtil {

  private FontUtil() { }
  
  /**
   * Changes the font style of the control to bold.
   */
  public static void convertFontToBold(Control control) {
    control.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
  }

}
