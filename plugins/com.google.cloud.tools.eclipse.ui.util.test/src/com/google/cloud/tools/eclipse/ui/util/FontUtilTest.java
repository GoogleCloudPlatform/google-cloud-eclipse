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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Label;
import org.junit.Rule;
import org.junit.Test;

public class FontUtilTest {

  @Rule public ShellTestResource shellTestResource = new ShellTestResource();
  
  @Test
  public void testConvertFontToBold() {
    Label label = new Label(shellTestResource.getShell(), SWT.NONE);
    for (FontData fontData : label.getFont().getFontData()) {
      assertThat(fontData.getStyle(), is(not(SWT.BOLD)));
    }
    FontUtil.convertFontToBold(label);
    for (FontData fontData : label.getFont().getFontData()) {
      assertThat(fontData.getStyle(), is(SWT.BOLD));
    }
  }

  @Test
  public void testConvertFontToItalic() {
    Label label = new Label(shellTestResource.getShell(), SWT.NONE);
    for (FontData fontData : label.getFont().getFontData()) {
      assertThat(fontData.getStyle(), is(not(SWT.ITALIC)));
    }
    FontUtil.convertFontToItalic(label);
    for (FontData fontData : label.getFont().getFontData()) {
      assertThat(fontData.getStyle(), is(SWT.ITALIC));
    }
  }
}
