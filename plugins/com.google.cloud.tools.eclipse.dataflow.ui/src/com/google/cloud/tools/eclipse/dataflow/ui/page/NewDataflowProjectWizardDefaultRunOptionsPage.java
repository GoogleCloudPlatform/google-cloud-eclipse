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

package com.google.cloud.tools.eclipse.dataflow.ui.page;

import com.google.cloud.tools.eclipse.dataflow.core.preferences.DataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.core.preferences.ProjectOrWorkspaceDataflowPreferences;
import com.google.cloud.tools.eclipse.dataflow.ui.preferences.RunOptionsDefaultsComponent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * An optional Page to input default run options for a Dataflow Project.
 */
public class NewDataflowProjectWizardDefaultRunOptionsPage extends WizardPage {
  private static final String PAGE_NAME = Messages.getString("RUN_OPTIONS");

  private RunOptionsDefaultsComponent runOptionsDefaultsComponent;

  public NewDataflowProjectWizardDefaultRunOptionsPage() {
    super(PAGE_NAME);
    setTitle(Messages.getString("SET_RUN_OPTIONS"));
    setDescription(Messages.getString("DATAFLOW_PIPELINE_OPTIONS"));
    setPageComplete(true);
  }

  @Override
  public void createControl(Composite parent) {
    DataflowPreferences prefs = ProjectOrWorkspaceDataflowPreferences.forWorkspace();
    Composite composite = new Composite(parent, SWT.NULL);
    int numColumns = 3;
    composite.setLayout(new GridLayout(numColumns, false));
    runOptionsDefaultsComponent = new RunOptionsDefaultsComponent(
        composite, numColumns, new DialogPageMessageTarget(this), prefs, this);

    setControl(runOptionsDefaultsComponent.getControl());
  }

  public String getAccountEmail() {
    return runOptionsDefaultsComponent.getAccountEmail();
  }

  public String getProjectId() {
    return runOptionsDefaultsComponent.getProject();
  }

  /**
   * @return name of the GCS bucket to stage artifacts in
   */
  public String getStagingLocation() {
    return runOptionsDefaultsComponent.getStagingLocation();
  }

}

