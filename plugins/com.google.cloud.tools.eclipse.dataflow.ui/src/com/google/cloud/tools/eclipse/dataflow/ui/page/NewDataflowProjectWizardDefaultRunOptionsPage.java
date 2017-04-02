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
import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowProjectCreator;
import com.google.cloud.tools.eclipse.dataflow.ui.preferences.RunOptionsDefaultsComponent;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * An optional Page to input default run options for a Dataflow Project.
 */
public class NewDataflowProjectWizardDefaultRunOptionsPage extends WizardPage {
  private static final String PAGE_NAME = "Dataflow Default Run Options";

  private final DataflowProjectCreator creator;

  private RunOptionsDefaultsComponent runOptionsDefaultsComponent;

  public NewDataflowProjectWizardDefaultRunOptionsPage(DataflowProjectCreator creator) {
    super(PAGE_NAME);
    this.creator = creator;
    setTitle("Set Default Cloud Dataflow Run Options");
    setDescription("Set default options for running a Dataflow Pipeline.");
    // This page is optional
    setPageComplete(true);
  }

  @Override
  public void createControl(Composite parent) {
    DataflowPreferences prefs = ProjectOrWorkspaceDataflowPreferences.forWorkspace();
    Composite composite = new Composite(parent, SWT.NULL);
    final int numColumns = 3;
    composite.setLayout(new GridLayout(numColumns, false));
    runOptionsDefaultsComponent = new RunOptionsDefaultsComponent(
        composite, numColumns, new DialogPageMessageTarget(this), prefs);

    setControl(runOptionsDefaultsComponent.getControl());
    addListeners();
  }

  private void addListeners() {
    runOptionsDefaultsComponent.addProjectModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        creator.setDefaultProject(runOptionsDefaultsComponent.getProject());
      }
    });
    runOptionsDefaultsComponent.addStagingLocationModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        creator.setDefaultStagingLocation(runOptionsDefaultsComponent.getStagingLocation());
      }
    });
  }

}

