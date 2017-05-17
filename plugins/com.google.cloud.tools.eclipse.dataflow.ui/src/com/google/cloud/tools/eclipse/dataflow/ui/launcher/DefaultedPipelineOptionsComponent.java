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

import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.ui.page.MessageTarget;
import com.google.cloud.tools.eclipse.dataflow.ui.preferences.RunOptionsDefaultsComponent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import java.util.HashMap;
import java.util.Map;

/**
 * A Component that contains a group of pipeline options that can be defaulted with {@link
 * DataflowPreferences}. Contains a Button (with Checkbox style) to enable and disable use of
 * defaults and a {@link RunOptionsDefaultsComponent} to input said defaults.
 */
public class DefaultedPipelineOptionsComponent {
  private Group defaultsGroup;

  @VisibleForTesting
  Button useDefaultsButton;

  private DataflowPreferences preferences;
  private Map<String, String> customValues;

  @VisibleForTesting
  RunOptionsDefaultsComponent defaultOptions;

  public DefaultedPipelineOptionsComponent(Composite parent, Object layoutData,
      MessageTarget messageTarget, DataflowPreferences preferences) {
    this.preferences = preferences;
    customValues = new HashMap<>();

    defaultsGroup = new Group(parent, SWT.NULL);
    int numColumns = 3;
    defaultsGroup.setLayout(new GridLayout(numColumns, false));
    defaultsGroup.setLayoutData(layoutData);

    useDefaultsButton = new Button(defaultsGroup, SWT.CHECK);
    useDefaultsButton.setText("Use &default Dataflow options");

    useDefaultsButton.addSelectionListener(new SetInputsEnabledOppositeButtonSelectionListener());
    useDefaultsButton.addSelectionListener(new SetInputValuesToDefaultOrCustomSelectionListener());

    useDefaultsButton.setLayoutData(
        new GridData(SWT.BEGINNING, SWT.CENTER, true, false, numColumns, 1));

    defaultOptions =
        new RunOptionsDefaultsComponent(defaultsGroup, numColumns, messageTarget, preferences);
  }

  public void setUseDefaultValues(boolean useDefaultValues) {
    useDefaultsButton.setSelection(useDefaultValues);
    setWidgetsEnabled(!useDefaultValues);
  }

  public void setCustomValues(Map<String, String> customValues) {
    this
        .customValues.put(
            DataflowPreferences.PROJECT_PROPERTY,
            customValues.get(DataflowPreferences.PROJECT_PROPERTY));
    this
        .customValues.put(
            DataflowPreferences.STAGING_LOCATION_PROPERTY,
            customValues.get(DataflowPreferences.STAGING_LOCATION_PROPERTY));
    // TODO: Select appropriate defaults based on major version
    this
        .customValues.put(
            DataflowPreferences.GCP_TEMP_LOCATION_PROPERTY,
            customValues.get(DataflowPreferences.STAGING_LOCATION_PROPERTY));
  }

  public void setPreferences(DataflowPreferences preferences) {
    this.preferences = preferences;
    // The default values may have changed, so ensure the input components are set to the
    // appropriate values.
    updateDefaultableInputValues();
  }

  private void setWidgetsEnabled(boolean enabled) {
    defaultOptions.setEnabled(enabled);
  }

  public Map<String, String> getValues() {
    Map<String, String> values = new HashMap<>();
    values.put(DataflowPreferences.PROJECT_PROPERTY, defaultOptions.getProject());
    values.put(DataflowPreferences.STAGING_LOCATION_PROPERTY, defaultOptions.getStagingLocation());
    // TODO: Give this a separate input
    values.put(DataflowPreferences.GCP_TEMP_LOCATION_PROPERTY, defaultOptions.getStagingLocation());
    return values;
  }

  public boolean isUseDefaultOptions() {
    return useDefaultsButton.getSelection();
  }

  /**
   * If the dataflow preferences have changed or the inputs have been enabled or disabled, the
   * values of the input components must be updated to the values that will be used.
   */
  private void updateDefaultableInputValues() {
    if (isUseDefaultOptions()) {
      customValues.put(DataflowPreferences.PROJECT_PROPERTY, defaultOptions.getProject());
      customValues.put(
          DataflowPreferences.STAGING_LOCATION_PROPERTY, defaultOptions.getStagingLocation());
      // TODO: Give this a separate input
      customValues.put(
          DataflowPreferences.GCP_TEMP_LOCATION_PROPERTY, defaultOptions.getStagingLocation());
      String defaultProject = preferences.getDefaultProject();
      defaultOptions.setCloudProjectText(defaultProject == null ? "" : defaultProject);
      String defaultStagingLocation = preferences.getDefaultStagingLocation();
      defaultOptions.setStagingLocationText(
          defaultStagingLocation == null ? "" : defaultStagingLocation);
    } else {
      String project = customValues.get(DataflowPreferences.PROJECT_PROPERTY);
      if (!Strings.isNullOrEmpty(project)) {
        defaultOptions.setCloudProjectText(project);
      }
      String stagingLocation = customValues.get(DataflowPreferences.STAGING_LOCATION_PROPERTY);
      if (!Strings.isNullOrEmpty(stagingLocation)) {
        defaultOptions.setStagingLocationText(stagingLocation);
      }
    }
  }

  public void addButtonSelectionListener(SelectionListener listener) {
    useDefaultsButton.addSelectionListener(listener);
  }

  public void addModifyListener(ModifyListener listener) {
    defaultOptions.addModifyListener(listener);
  }

  /**
   * Set the enablement of the custom value inputs to the opposite of the {@code useDefaultsButton}.
   */
  private class SetInputsEnabledOppositeButtonSelectionListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      setWidgetsEnabled(!useDefaultsButton.getSelection());
    }
  }

  /**
   * When the {@code useDefaultsButton} is selected, set the current text to the default values, and
   * when it is unselected, if custom values exist, set the current text to the custom values.
   */
  private class SetInputValuesToDefaultOrCustomSelectionListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      updateDefaultableInputValues();
    }

  }
}
