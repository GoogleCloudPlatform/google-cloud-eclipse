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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

public abstract class AppEngineProjectWizard extends Wizard implements INewWizard {

  private final AppEngineWizardPage appEnginePage;
  protected final AppEngineProjectConfig config = new AppEngineProjectConfig();
  private IWorkbench workbench;

  public AppEngineProjectWizard(AppEngineWizardPage appEngineWizardPage) {
    appEnginePage = Preconditions.checkNotNull(appEngineWizardPage);
    addPage(appEnginePage);
    setNeedsProgressMonitor(true);
  }

  public abstract CreateAppEngineWtpProject getAppEngineProjectCreationOperation(
      AppEngineProjectConfig config, IAdaptable uiInfoAdapter);

  @Override
  public void addPages() {
    // Clear interrupted state
    // (https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2064)
    Thread.interrupted();
  }

  @Override
  public boolean performFinish() {
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
    } catch (InterruptedException | InvocationTargetException ex) {
      String message = Messages.getString("project.creation.failed"); //$NON-NLS-1$
      StatusUtil.setErrorStatus(this, message, ex);
      return false;
    }
  }

  private void retrieveConfigurationValues() {
    config.setServiceName(appEnginePage.getServiceName());
    config.setPackageName(appEnginePage.getPackageName());
    config.setRuntimeId(appEnginePage.getRuntimeId());
    config.setProject(appEnginePage.getProjectHandle());
    if (!appEnginePage.useDefaults()) {
      config.setEclipseProjectLocationUri(appEnginePage.getLocationURI());
    }

    config.setAppEngineLibraries(appEnginePage.getSelectedLibraries());

    if (appEnginePage.asMavenProject()) {
      config.setUseMaven(appEnginePage.getMavenGroupId(), appEnginePage.getMavenArtifactId(),
          appEnginePage.getMavenVersion());
    }
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.workbench = workbench;
  }
}
