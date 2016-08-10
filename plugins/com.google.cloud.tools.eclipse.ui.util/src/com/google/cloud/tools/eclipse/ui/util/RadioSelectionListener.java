package com.google.cloud.tools.eclipse.ui.util;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;

public class RadioSelectionListener<C extends Control> implements SelectionListener {

  private C target;
  private Button button;

  public RadioSelectionListener(C text, Button button) {
    this.target = text;
    this.button = button;
  }

  @Override
  public void widgetSelected(SelectionEvent event) {
    doSelection(event);
  }

  @Override
  public void widgetDefaultSelected(SelectionEvent event) {
    doSelection(event);
  }

  private void doSelection(SelectionEvent event) {
    target.setEnabled(event.getSource() == button);
  }
}