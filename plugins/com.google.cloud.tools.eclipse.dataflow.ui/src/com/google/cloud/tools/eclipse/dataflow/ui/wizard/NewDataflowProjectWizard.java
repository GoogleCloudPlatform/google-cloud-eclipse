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

package com.google.cloud.tools.eclipse.dataflow.ui.wizard;

import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowProjectCreator;
import com.google.cloud.tools.eclipse.dataflow.ui.page.NewDataflowProjectWizardLandingPage;
import com.google.cloud.tools.eclipse.dataflow.ui.DataflowUiPlugin;
import com.google.cloud.tools.eclipse.dataflow.ui.page.NewDataflowProjectWizardDefaultRunOptionsPage;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import java.lang.reflect.InvocationTargetException;

/**
 * A Wizard to create a new Google Cloud Dataflow Project.
 */
public class NewDataflowProjectWizard extends Wizard implements INewWizard {
  private DataflowProjectCreator creator = DataflowProjectCreator.create();

  private NewDataflowProjectWizardLandingPage landingPage;
  private NewDataflowProjectWizardDefaultRunOptionsPage defaultRunOptionsPage;

  @Override
  public boolean performFinish() {
    if (!creator.isValid()) {
      String message =
          "Tried to finish the New Dataflow Project Wizard "
              + "when the project creator is not valid. Reasons: " + creator.validate();
      IllegalStateException ex = new IllegalStateException(message);
      DataflowUiPlugin.logError(ex, message);
      throw ex;
    }
    try {
      getContainer().run(true, true, creator);
    } catch (InvocationTargetException | InterruptedException e) {
      // TODO: handle
      DataflowUiPlugin.logError(e, "Error encountered when trying to create project");
      return false;
    }
    return true;
  }

  @Override
  public void addPages() {
    landingPage = new NewDataflowProjectWizardLandingPage(creator);
    addPage(landingPage);

    defaultRunOptionsPage = new NewDataflowProjectWizardDefaultRunOptionsPage(creator);
    addPage(defaultRunOptionsPage);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    setHelpAvailable(false);
    setWindowTitle("New Cloud Dataflow Project");
    setNeedsProgressMonitor(true);
  }
}
