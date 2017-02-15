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
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineComponentPage;
import com.google.cloud.tools.eclipse.sdk.ui.preferences.CloudSdkPrompter;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.statushandlers.StatusManager;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class StandardProjectWizard extends Wizard implements INewWizard {

  private AppEngineStandardWizardPage page;
  private AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();

  public StandardProjectWizard() {
    this.setWindowTitle("New App Engine Standard Project");
    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    if (appEngineJavaComponentExists()) {
      page = new AppEngineStandardWizardPage();
      this.addPage(page);
    } else {
      this.addPage(new AppEngineComponentPage(true /* forNativeProjectWizard */));
    }
  }

  @Override
  public boolean performFinish() {
    AnalyticsPingManager.getInstance().sendPing(
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_COMPLETE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_NATIVE);

    if (config.getCloudSdkLocation() == null) {
      File location = CloudSdkPrompter.getCloudSdkLocation(getShell());
      if (location == null) {
        return false;
      }
      config.setCloudSdkLocation(location);
    }

    config.setPackageName(page.getPackageName());
    config.setProject(page.getProjectHandle());
    if (!page.useDefaults()) {
      config.setEclipseProjectLocationUri(page.getLocationURI());
    }

    config.setAppEngineLibraries(page.getSelectedLibraries());

    // todo set up
    final IAdaptable uiInfoAdapter = WorkspaceUndoUtil.getUIInfoAdapter(getShell());
    IRunnableWithProgress runnable = new CreateAppEngineStandardWtpProject(config, uiInfoAdapter);

    IStatus status = Status.OK_STATUS;
    try {
      boolean fork = true;
      boolean cancelable = true;
      getContainer().run(fork, cancelable, runnable);
    } catch (InterruptedException ex) {
      status = Status.CANCEL_STATUS;
    } catch (InvocationTargetException ex) {
      status = setErrorStatus(ex.getCause());
    }

    return status.isOK();
  }

  // visible for testing
  static IStatus setErrorStatus(Throwable ex) {
    int errorCode = 1;
    String message = "Failed to create project";
    if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
      message += ": " + ex.getMessage();
    }
    IStatus status = new Status(Status.ERROR, "todo plugin ID", errorCode, message, ex);
    StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
    return status;
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
  }

  private boolean appEngineJavaComponentExists() {
    try {
      new CloudSdk.Builder().build().validateAppEngineJavaComponents();
      return true;
    } catch (AppEngineJavaComponentsNotInstalledException ex) {
      return false;
    }
  }

}
