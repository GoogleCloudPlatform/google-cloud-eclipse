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

import com.google.common.base.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A collection that maps Labels to user-editable texts. The labels are all of equal width and
 * right-aligned.
 */
public class LabeledTextMapComponent {

  private final FormToolkit toolkit;
  private final Composite parent;
  private final GridData gridData;

  private Composite composite;
  private Map<String, Text> texts;
  private String argumentsSeparator;

  private Collection<ModifyListener> textModifyListeners;

  /**
   * Creates a new LabeledTextMapComponent as a child of the parent with the provided Grid Data
   * layout for the overall component. Each label will be separated from the Text input with the
   * provided Argument Separator.
   */
  public LabeledTextMapComponent(FormToolkit toolkit, Composite parent, GridData gridData,
      String argumentsSeparator) {
    this.toolkit = toolkit;
    this.parent = parent;
    this.gridData = gridData;
    this.texts = new LinkedHashMap<>();
    init();

    this.argumentsSeparator = argumentsSeparator;
    this.textModifyListeners = new HashSet<>();
  }

  private void init() {
    composite = toolkit.createComposite(parent, SWT.NULL);
    Layout layout = new GridLayout(3, false);
    composite.setLayout(layout);
    composite.setLayoutData(gridData);
    texts.clear();
    parent.layout();
  }

  public void reinit() {
    composite.dispose();
    parent.layout();
    init();
  }

  public Control getControl() {
    return composite;
  }

  /**
   * Add a modify listener that triggers whenever a text within this component is modified.
   */
  public void addModifyListener(ModifyListener listener) {
    textModifyListeners.add(listener);
    for (Text text : texts.values()) {
      text.addModifyListener(listener);
    }
  }

  /**
   * Add a new text input to this component with the specified label.
   */
  public void addLabeledText(String label, Optional<String> tooltip) {
    Label newLabel = addLabel(label);
    Label newSeparator = addSeparator();


    Text newText = new Text(composite, SWT.SINGLE | SWT.BORDER);
    GridData textGridData = singleColumnGridData();
    textGridData.grabExcessHorizontalSpace = true;
    newText.setLayoutData(textGridData);

    for (ModifyListener listener : textModifyListeners) {
      newText.addModifyListener(listener);
    }

    newLabel.setToolTipText(tooltip.or(""));
    newSeparator.setToolTipText(tooltip.or(""));
    newText.setToolTipText(tooltip.or(""));

    texts.put(label, newText);
  }

  private Label addLabel(String label) {
    Label newLabel = toolkit.createLabel(composite, label, SWT.NULL);
    newLabel.setText(label);
    GridData labelGridData = singleColumnGridData();
    labelGridData.horizontalAlignment = SWT.END;
    labelGridData.verticalAlignment = SWT.CENTER;
    labelGridData.grabExcessHorizontalSpace = false;
    newLabel.setLayoutData(labelGridData);
    toolkit.createLabel(parent, label);
    return newLabel;
  }

  private Label addSeparator() {
    Label separator = toolkit.createLabel(composite, argumentsSeparator, SWT.NULL);
    separator.setText(argumentsSeparator);
    GridData separatorGridData = singleColumnGridData();
    separatorGridData.grabExcessHorizontalSpace = false;
    separatorGridData.verticalAlignment = SWT.CENTER;
    separator.setLayoutData(separatorGridData);
    return separator;
  }

  /**
   * Get a map from the Label value of each text to the user-provided contents of that text.
   */
  public Map<String, String> getTextValues() {
    Map<String, String> textValues = new HashMap<>();
    for (Map.Entry<String, Text> text : texts.entrySet()) {
      textValues.put(text.getKey(), text.getValue().getText());
    }
    return Collections.unmodifiableMap(textValues);
  }

  /**
   * For each map entry in the provided map, if the key is present as a label within this component,
   * set the text to the associated value. Labels that are present in the provided map but not in
   * the component are ignored.
   */
  public void setTextValuesForExistingLabels(Map<String, String> labeledValues) {
    for (Map.Entry<String, String> labeledValueEntry : labeledValues.entrySet()) {
      if (texts.containsKey(labeledValueEntry.getKey())) {
        texts.get(labeledValueEntry.getKey()).setText(labeledValueEntry.getValue());
      }
    }
  }

  private static GridData singleColumnGridData() {
    GridData data = new GridData();
    data.horizontalSpan = 1;
    data.verticalSpan = 1;
    data.horizontalAlignment = SWT.FILL;
    data.verticalAlignment = SWT.FILL;
    return data;
  }

}
