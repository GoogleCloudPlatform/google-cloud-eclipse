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

package com.google.cloud.tools.eclipse.dataflow.ui.preferences;

import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.WritableDataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.ui.page.DialogPageMessageTarget;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;

/**
 * Collects default run options for Dataflow Pipelines and provides means to create and modify them
 * in an underlying {@link DataflowPreferences} implementation.
 */
public class DefaultRunOptionsPage
    extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
  private RunOptionsDefaultsComponent runOptionsComponent;
  private WritableDataflowPreferences preferences;

  private IProject selectedProject;

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    composite.setLayout(new GridLayout(1, false));
    Group group = new Group(composite, SWT.NULL);
    group.setText("Execution Options for Google Cloud Platform");
    int numColumns = 3;
    group.setLayout(new GridLayout(numColumns, false));
    group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    runOptionsComponent = new RunOptionsDefaultsComponent(
        group, numColumns, new DialogPageMessageTarget(this), preferences);

    return composite;
  }

  @Override
  public boolean performOk() {
    updatePreferencesFromInputs();
    preferences.save();
    return super.performOk();
  }

  /**
   * Updates the internal preferences from the input elements.
   */
  private void updatePreferencesFromInputs() {
    preferences.setDefaultProject(runOptionsComponent.getProject());
    preferences.setDefaultStagingLocation(runOptionsComponent.getStagingLocation());
  }

  @Override
  public void init(IWorkbench workbench) {
    this.preferences = WritableDataflowPreferences.global();
  }

  @Override
  public IAdaptable getElement() {
    return selectedProject;
  }

  @Override
  public void setElement(IAdaptable element) {
    this.selectedProject = element.getAdapter(IProject.class);
    if (selectedProject == null) {
      throw new IllegalArgumentException(
          "Provided element " + element + " isn't adaptable to a project.");
    } else {
      this.preferences = WritableDataflowPreferences.forProject(this.selectedProject);
    }
  }

}
