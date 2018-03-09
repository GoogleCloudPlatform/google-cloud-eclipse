/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.newproject;

import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Preconditions;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

public abstract class AppEngineProjectWizard extends Wizard implements INewWizard {

  private final AppEngineWizardPage wizardPage;
  protected final AppEngineProjectConfig config = new AppEngineProjectConfig();
  private IWorkbench workbench;

  public AppEngineProjectWizard(AppEngineWizardPage appEngineWizardPage) {
    wizardPage = Preconditions.checkNotNull(appEngineWizardPage);
    addPage(wizardPage);
    setNeedsProgressMonitor(true);
  }

  public abstract void sendAnalyticsPing();

  public abstract IStatus validateDependencies();

  public abstract CreateAppEngineWtpProject getAppEngineProjectCreationOperation(
      AppEngineProjectConfig config, IAdaptable uiInfoAdapter);

  @Override
  public void addPages() {
    // Clear interrupted state
    // (https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2064)
    Thread.interrupted();

    sendAnalyticsPing();
  }

  @Override
  public boolean performFinish() {
    IStatus status = validateDependencies();
    if (!status.isOK()) {
      StatusUtil.setErrorStatus(this, status.getMessage(), status);
      return false;
    }

    retrieveConfigurationValues();

    // todo set up
    IAdaptable uiInfoAdapter = WorkspaceUndoUtil.getUIInfoAdapter(getShell());
    CreateAppEngineWtpProject runnable =
        getAppEngineProjectCreationOperation(config, uiInfoAdapter);

    try {
      boolean fork = true;
      boolean cancelable = true;
      getContainer().run(fork, cancelable, runnable);

      // open most important file created by wizard in editor
      IFile file = runnable.getMostImportant();
      WorkbenchUtil.openInEditor(workbench, file);
      return true;
    } catch (InterruptedException ex) {
      return false;
    } catch (InvocationTargetException ex) {
      String message = Messages.getString("project.creation.failed"); //$NON-NLS-1$
      StatusUtil.setErrorStatus(this, message, ex.getCause());
      return false;
    }
  }

  private void retrieveConfigurationValues() {
    config.setServiceName(wizardPage.getServiceName());
    config.setPackageName(wizardPage.getPackageName());
    config.setRuntimeId(wizardPage.getRuntimeId());
    config.setProject(wizardPage.getProjectHandle());
    if (!wizardPage.useDefaults()) {
      config.setEclipseProjectLocationUri(wizardPage.getLocationURI());
    }

    config.setAppEngineLibraries(wizardPage.getSelectedLibraries());

    if (wizardPage.asMavenProject()) {
      config.setUseMaven(wizardPage.getMavenGroupId(), wizardPage.getMavenArtifactId(), wizardPage.getMavenVersion());
    }
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.workbench = workbench;
  }
}
