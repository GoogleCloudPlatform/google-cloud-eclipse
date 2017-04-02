/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.page.component;

import com.google.cloud.tools.eclipse.dataflow.ui.util.ButtonFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * A layout with a textbox and a button.
 *
 * <p>Useful for things like class selectors, with context provided by enclosing elements.
 */
public class TextAndButtonComponent {
  private Text text;
  private Button button;

  public TextAndButtonComponent(Composite parent, GridData layoutData, String buttonText) {
    Composite composite = new Composite(parent, SWT.NULL);
    composite.setLayoutData(layoutData);
    composite.setLayout(new GridLayout(2, false));

    text = new Text(composite, SWT.BORDER | SWT.SINGLE);
    text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    button = ButtonFactory.newPushButton(composite, buttonText);
  }

  public String getText() {
    return text.getText();
  }

  public void setText(String value) {
    text.setText(value);
  }

  public void setEnabled(boolean enabled) {
    text.setEnabled(enabled);
    button.setEnabled(enabled);
  }

  public void addButtonSelectionListener(TextAndButtonSelectionListener listener) {
    listener.init(text, button);
    button.addSelectionListener(listener);
  }

  public void addTextModifiedListener(ModifyListener listener) {
    text.addModifyListener(listener);
  }
}
