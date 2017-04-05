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

package com.google.cloud.tools.eclipse.dataflow.ui.launcher;

import com.google.cloud.tools.eclipse.dataflow.core.launcher.PipelineLaunchConfiguration;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsProperty;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsType;
import com.google.cloud.tools.eclipse.dataflow.ui.page.component.LabeledTextMapComponent;
import com.google.common.base.Optional;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A Form containing collapsable inputs for {@code PipelineOptions} inputs.
 */
public class PipelineOptionsFormComponent {
  private static final String TEMP_LOCATION_PROPERTY = "tempLocation";

  private static final String REQURIED_ARGUMENTS_SECTION_NAME = "Required Arguments";

  private final Set<ModifyListener> modifyListeners = new HashSet<>();
  private final Set<IExpansionListener> expansionListeners = new HashSet<>();

  private final String argumentSeparator;
  private final Set<String> filterProperties;

  private final Composite parent;
  private FormToolkit formToolkit;
  private Form form;
  private Map<ExpandableComposite, LabeledTextMapComponent> optionsComponents =
      new LinkedHashMap<>();

  public PipelineOptionsFormComponent(
      Composite parent, String argumentSeparator, Set<String> filterProperties) {
    this.argumentSeparator = argumentSeparator;
    this.filterProperties = filterProperties;

    this.parent = parent;

    formToolkit = new FormToolkit(parent.getDisplay());
    formToolkit.setBackground(parent.getBackground());
  }

  public void updateForm(
      PipelineLaunchConfiguration launchConfiguration,
      Map<PipelineOptionsType, Set<PipelineOptionsProperty>> types) {
    resetForm();
    if (launchConfiguration == null || launchConfiguration.getRunner() == null) {
      return;
    }

    addTypesToForm(launchConfiguration, types);

    parent.layout();
  }

  private void resetForm() {
    if (form != null) {
      form.dispose();
    }
    optionsComponents = new LinkedHashMap<>();
    form = formToolkit.createForm(parent);
    form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    form.getBody().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    form.getBody().setLayout(new GridLayout());
  }

  private void addTypesToForm(PipelineLaunchConfiguration launchConfiguration,
      Map<PipelineOptionsType, Set<PipelineOptionsProperty>> optionsByType) {
    Multimap<String, String> options = LinkedHashMultimap.create();
    Map<String, Optional<String>> optionsDescriptions = new HashMap<>();
    Set<String> requiredOptions = new LinkedHashSet<>();

    for (Map.Entry<PipelineOptionsType, Set<PipelineOptionsProperty>> optionsTypeEntry :
        optionsByType.entrySet()) {
      if (optionsTypeEntry.getValue().isEmpty()) {
        continue;
      }
      PipelineOptionsType optionsType = optionsTypeEntry.getKey();

      for (PipelineOptionsProperty property : optionsTypeEntry.getValue()) {
        String propertyName = property.getName();
        if (!filterProperties.contains(propertyName)) {
          if (property.isUserValueRequired()
              && !property.getName().equals(TEMP_LOCATION_PROPERTY)) {
            requiredOptions.add(propertyName);
          } else {
            options.put(optionsType.getName(), propertyName);
          }
          optionsDescriptions.put(propertyName, Optional.fromNullable(property.getDescription()));
        }
      }
    }

    if (!requiredOptions.isEmpty()) {
      optionsTypeSection(launchConfiguration, REQURIED_ARGUMENTS_SECTION_NAME, requiredOptions,
          optionsDescriptions, SWT.NULL);
    }

    for (Map.Entry<String, Collection<String>> optionsTypeProperty : options.asMap().entrySet()) {
      if (!optionsTypeProperty.getValue().isEmpty()) {
        optionsTypeSection(launchConfiguration, optionsTypeProperty.getKey(),
            optionsTypeProperty.getValue(), optionsDescriptions, ExpandableComposite.TWISTIE);
      }
    }
  }

  private ExpandableComposite optionsTypeSection(PipelineLaunchConfiguration launchConfiguration,
      String optionsTypeName, Collection<String> optionsTypeProperties,
      Map<String, Optional<String>> optionsDescriptions, int style) {
    ExpandableComposite expandable = formToolkit.createSection(form.getBody(), style);
    expandable.setLayout(new GridLayout());
    expandable.setBackground(parent.getBackground());
    expandable.setForeground(parent.getForeground());
    expandable.setText(optionsTypeName);
    expandable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    LabeledTextMapComponent typeArgs = new LabeledTextMapComponent(formToolkit, expandable,
        new GridData(SWT.FILL, SWT.CENTER, true, false), argumentSeparator);

    for (ModifyListener modifyListener : modifyListeners) {
      typeArgs.addModifyListener(modifyListener);
    }
    for (IExpansionListener expandListener : expansionListeners) {
      expandable.addExpansionListener(expandListener);
    }
    for (String property : optionsTypeProperties) {
      typeArgs.addLabeledText(property, optionsDescriptions.get(property));
    }

    optionsComponents.put(expandable, typeArgs);
    expandable.setClient(typeArgs.getControl());
    typeArgs.setTextValuesForExistingLabels(launchConfiguration.getArgumentValues());

    return expandable;
  }

  public Collection<LabeledTextMapComponent> getComponents() {
    return Collections.unmodifiableCollection(optionsComponents.values());
  }

  public void addModifyListener(ModifyListener listener) {
    modifyListeners.add(listener);
    for (LabeledTextMapComponent component : optionsComponents.values()) {
      component.addModifyListener(listener);
    }
  }

  public void addExpandListener(IExpansionListener expandListener) {
    for (ExpandableComposite expandable : optionsComponents.keySet()) {
      expandable.addExpansionListener(expandListener);
    }
    expansionListeners.add(expandListener);
  }
}
