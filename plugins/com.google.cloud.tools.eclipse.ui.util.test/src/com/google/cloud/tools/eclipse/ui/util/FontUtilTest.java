package com.google.cloud.tools.eclipse.ui.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

public class FontUtilTest {

  @Test
  public void test() {
    Shell shell = new Shell();
    Label label = new Label(shell, SWT.NONE);
    for (FontData fontData : label.getFont().getFontData()) {
      assertThat(fontData.getStyle(), is(not(SWT.BOLD)));
    }
    FontUtil.convertFontToBold(label);
    for (FontData fontData : label.getFont().getFontData()) {
      assertThat(fontData.getStyle(), is(SWT.BOLD));
    }
  }

}
