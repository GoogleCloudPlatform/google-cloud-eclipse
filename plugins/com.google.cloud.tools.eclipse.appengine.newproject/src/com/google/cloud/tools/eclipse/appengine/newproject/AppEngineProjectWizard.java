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

import com.google.cloud.tools.appengine.cloudsdk.AppEngineJavaComponentsNotInstalledException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkOutOfDateException;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineJavaComponentMissingPage;
import com.google.cloud.tools.eclipse.appengine.ui.CloudSdkMissingPage;
import com.google.cloud.tools.eclipse.appengine.ui.CloudSdkOutOfDatePage;
import com.google.cloud.tools.eclipse.sdk.ui.preferences.CloudSdkPrompter;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

public abstract class AppEngineProjectWizard extends Wizard implements INewWizard {

  private AppEngineWizardPage page = null;
  private AppEngineProjectConfig config = new AppEngineProjectConfig();
  private IWorkbench workbench;

  public AppEngineProjectWizard() {
    setNeedsProgressMonitor(true);
  }

  public abstract AppEngineWizardPage createWizardPage();

  public abstract void sendAnalyticsPing();

  public abstract IStatus validateDependencies(boolean fork, boolean cancelable);

  public abstract CreateAppEngineWtpProject getAppEngineProjectCreationOperation(
      AppEngineProjectConfig config, IAdaptable uiInfoAdapter);

  @Override
  public void addPages() {
    try {
      CloudSdk sdk = new CloudSdk.Builder().build();
      sdk.validateCloudSdk();
      sdk.validateAppEngineJavaComponents();
      page = createWizardPage();
      addPage(page);
    } catch (CloudSdkNotFoundException ex) {
      addPage(new CloudSdkMissingPage(AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_NATIVE));
    } catch (CloudSdkOutOfDateException ex) {
      addPage(new CloudSdkOutOfDatePage(AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_NATIVE));
    } catch (AppEngineJavaComponentsNotInstalledException ex) {
      addPage(new AppEngineJavaComponentMissingPage(
          AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_NATIVE));
    }
  }

  @Override
  public boolean performFinish() {
    sendAnalyticsPing();

    if (page == null) {
      return true;
    }

    boolean fork = true;
    boolean cancelable = true;
    IStatus status = validateDependencies(fork, cancelable);
    if (!status.isOK()) {
      return false;
    }

    config.setServiceName(page.getServiceName());
    config.setPackageName(page.getPackageName());
    config.setProject(page.getProjectHandle());
    if (!page.useDefaults()) {
      config.setEclipseProjectLocationUri(page.getLocationURI());
    }

    config.setAppEngineLibraries(page.getSelectedLibraries());

    // todo set up
    IAdaptable uiInfoAdapter = WorkspaceUndoUtil.getUIInfoAdapter(getShell());
    CreateAppEngineWtpProject runnable = getAppEngineProjectCreationOperation(config, uiInfoAdapter);

    try {
      getContainer().run(fork, cancelable, runnable);
      
      // open most important file created by wizard in editor
      IFile file = runnable.getMostImportant();
      WorkbenchUtil.openInEditor(workbench, file);
    } catch (InterruptedException ex) {
      status = Status.CANCEL_STATUS;
    } catch (InvocationTargetException ex) {
      status = StatusUtil.setErrorStatus(this, Messages.getString("project.creation.failed"), ex.getCause());
    }

    return status.isOK();
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.workbench = workbench;
    if (config.getCloudSdkLocation() == null) {
      File location = CloudSdkPrompter.getCloudSdkLocation(getShell());
      // if the user doesn't provide the Cloud SDK then we'll error in performFinish() too
      if (location != null) {
        config.setCloudSdkLocation(location);
      }
    }
  }

}
