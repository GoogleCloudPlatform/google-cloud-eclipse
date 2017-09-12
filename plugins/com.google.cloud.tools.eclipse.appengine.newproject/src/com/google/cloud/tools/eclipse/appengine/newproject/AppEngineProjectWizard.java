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
import com.google.cloud.tools.eclipse.util.service.ServiceContextFactory;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Preconditions;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import javax.inject.Inject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/**
 * Base class for App Engine projection creation.
 * <p>
 * Expected to be created via the {@link ServiceContextFactory}.
 */
public abstract class AppEngineProjectWizard extends Wizard implements INewWizard {

  protected AppEngineWizardPage page = null;
  protected final AppEngineProjectConfig config = new AppEngineProjectConfig();
  private IWorkbench workbench;

  @Inject
  protected IConfigurationElement configElement;

  public AppEngineProjectWizard() {
    setNeedsProgressMonitor(true);
  }

  public abstract AppEngineWizardPage createWizardPage();

  public abstract IStatus validateDependencies();

  public abstract CreateAppEngineWtpProject getAppEngineProjectCreationOperation(
      AppEngineProjectConfig config, IAdaptable uiInfoAdapter);

  @Override
  public void addPages() {
    try {
      // Clear interrupted state
      // (https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2064)
      Thread.interrupted();
      CloudSdk sdk = new CloudSdk.Builder().build();
      sdk.validateCloudSdk();
      sdk.validateAppEngineJavaComponents();
      page = createWizardPage();
      addPage(page);
    } catch (CloudSdkNotFoundException ex) {
      addPage(new CloudSdkMissingPage());
    } catch (CloudSdkOutOfDateException ex) {
      addPage(new CloudSdkOutOfDatePage());
    } catch (AppEngineJavaComponentsNotInstalledException ex) {
      addPage(new AppEngineJavaComponentMissingPage());
    }
  }

  @Override
  public boolean performFinish() {
    Preconditions.checkState(page != null);

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

      // prompt to switch to preferred perspective
      BasicNewProjectResourceWizard.updatePerspective(configElement);

      // open most important file created by wizard in editor
      IFile file = runnable.getMostImportant();
      BasicNewProjectResourceWizard.selectAndReveal(file, workbench.getActiveWorkbenchWindow());
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

  protected void retrieveConfigurationValues() {
    config.setServiceName(page.getServiceName());
    config.setPackageName(page.getPackageName());
    config.setRuntimeId(page.getRuntimeId());
    config.setProject(page.getProjectHandle());
    if (!page.useDefaults()) {
      config.setEclipseProjectLocationUri(page.getLocationURI());
    }

    config.setAppEngineLibraries(page.getSelectedLibraries());

    if (page.asMavenProject()) {
      config.setUseMaven(page.getMavenGroupId(), page.getMavenArtifactId(), page.getMavenVersion());
    }
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
