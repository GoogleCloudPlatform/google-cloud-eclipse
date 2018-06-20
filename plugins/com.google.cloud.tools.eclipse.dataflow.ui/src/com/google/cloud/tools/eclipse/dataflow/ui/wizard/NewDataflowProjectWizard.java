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
import com.google.cloud.tools.eclipse.dataflow.ui.DataflowUiPlugin;
import com.google.cloud.tools.eclipse.dataflow.ui.Messages;
import com.google.cloud.tools.eclipse.dataflow.ui.page.NewDataflowProjectWizardDefaultRunOptionsPage;
import com.google.cloud.tools.eclipse.dataflow.ui.page.NewDataflowProjectWizardLandingPage;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * A Wizard to create a new Google Cloud Dataflow Project.
 */
public class NewDataflowProjectWizard extends Wizard implements INewWizard {
  private final DataflowProjectCreator creator = DataflowProjectCreator.create();

  private NewDataflowProjectWizardDefaultRunOptionsPage defaultRunOptionsPage;

  @Override
  public boolean performFinish() {
    AnalyticsPingManager.getInstance().sendPing(
        AnalyticsEvents.DATAFLOW_NEW_PROJECT_WIZARD_COMPLETE);

    creator.setDefaultAccountEmail(defaultRunOptionsPage.getAccountEmail());
    creator.setDefaultProject(defaultRunOptionsPage.getProjectId());
    creator.setDefaultStagingLocation(defaultRunOptionsPage.getStagingLocation());
    creator.setDefaultServiceAccountKey(defaultRunOptionsPage.getServiceAccountKey());

    if (!creator.isValid()) {
      String message =
          "Tried to finish the New Dataflow Project Wizard " //$NON-NLS-1$
              + "when the project creator is not valid. Reasons: " //$NON-NLS-1$ 
              + creator.validate();
      IllegalStateException ex = new IllegalStateException(message);
      DataflowUiPlugin.logError(ex, message);
      throw ex;
    }
    try {
      getContainer().run(true, true, creator);
      return true;
    } catch (InvocationTargetException | InterruptedException ex) {
      String message = "Error encountered when trying to create project"; //$NON-NLS-1$
      DataflowUiPlugin.logError(ex, message);
      StatusUtil.setErrorStatus(this, message, ex);
      return false;
    }
  }

  @Override
  public void addPages() {
    WizardPage landingPage = new NewDataflowProjectWizardLandingPage(creator);
    addPage(landingPage);

    defaultRunOptionsPage = new NewDataflowProjectWizardDefaultRunOptionsPage();
    addPage(defaultRunOptionsPage);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    setHelpAvailable(true);
    setWindowTitle(Messages.getString("new.cloud.dataflow.project")); //$NON-NLS-1$
    setNeedsProgressMonitor(true);
  }
}
