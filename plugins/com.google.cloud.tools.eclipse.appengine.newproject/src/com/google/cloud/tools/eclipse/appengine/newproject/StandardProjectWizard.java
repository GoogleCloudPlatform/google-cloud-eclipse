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

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineJavaComponentMissingPage;
import com.google.cloud.tools.eclipse.appengine.ui.CloudSdkMissingPage;
import com.google.cloud.tools.eclipse.sdk.ui.preferences.CloudSdkPrompter;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
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

public class StandardProjectWizard extends Wizard implements INewWizard {

  private AppEngineStandardWizardPage page = null;
  private AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();

  public StandardProjectWizard() {
    setWindowTitle(Messages.getString("new.app.engine.standard.project"));
    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    if (!cloudSdkExists()) {
      addPage(new CloudSdkMissingPage(AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_NATIVE));
    } else if (!appEngineJavaComponentExists()) {
      addPage(new AppEngineJavaComponentMissingPage(
          AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_NATIVE));
    } else { // all is good
      page = new AppEngineStandardWizardPage();
      addPage(page);
    }
  }

  @Override
  public boolean performFinish() {
    AnalyticsPingManager.getInstance().sendPing(
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_COMPLETE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_NATIVE);

    if (page == null) {
      return true;
    }

    config.setPackageName(page.getPackageName());
    config.setProject(page.getProjectHandle());
    if (!page.useDefaults()) {
      config.setEclipseProjectLocationUri(page.getLocationURI());
    }

    config.setAppEngineLibraries(page.getSelectedLibraries());

    // todo set up
    IAdaptable uiInfoAdapter = WorkspaceUndoUtil.getUIInfoAdapter(getShell());
    IRunnableWithProgress runnable = new CreateAppEngineStandardWtpProject(config, uiInfoAdapter);

    IStatus status = Status.OK_STATUS;
    try {
      boolean fork = true;
      boolean cancelable = true;
      getContainer().run(fork, cancelable, runnable);
    } catch (InterruptedException ex) {
      status = Status.CANCEL_STATUS;
    } catch (InvocationTargetException ex) {
      status = setErrorStatus(this, ex.getCause());
    }

    return status.isOK();
  }

  public static IStatus setErrorStatus(Object origin, Throwable ex) {
    String message = Messages.getString("project.creation.failed");
    if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
      message += ": " + ex.getMessage();
    }
    IStatus status = StatusUtil.error(origin, message, ex);
    StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
    return status;
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    if (config.getCloudSdkLocation() == null) {
      File location = CloudSdkPrompter.getCloudSdkLocation(getShell());
      // if the user doesn't provide the Cloud SDK then we'll error in performFinish() too
      if (location != null) {
        config.setCloudSdkLocation(location);
      }
    }
  }

  /**
   * Verify that the Cloud SDK is where we can find it.
   */
  public static boolean cloudSdkExists() {
    try {
      CloudSdk sdk = new CloudSdk.Builder().build();
      sdk.validateCloudSdk();
      return true;
    } catch (AppEngineException ex) {
      return false;
    }
  }

  /**
   * Verify that we're set up for App Engine Java development.
   */
  public static boolean appEngineJavaComponentExists() {
    try {
      CloudSdk sdk = new CloudSdk.Builder().build();
      sdk.validateAppEngineJavaComponents();
      return true;
    } catch (AppEngineException ex) {
      return false;
    }
  }

}
