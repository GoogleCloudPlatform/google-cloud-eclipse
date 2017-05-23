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

package com.google.cloud.tools.eclipse.appengine.newproject.maven;

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
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class MavenArchetypeProjectWizard extends Wizard implements INewWizard {
  private MavenAppEngineStandardWizardPage page;
  private MavenAppEngineStandardArchetypeWizardPage archetypePage;
  private File cloudSdkLocation;
  private IWorkbench workbench;

  public MavenArchetypeProjectWizard() {
    setWindowTitle(Messages.getString("WIZARD_TITLE")); //$NON-NLS-1$
    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    try {
      CloudSdk sdk = new CloudSdk.Builder().build();
      sdk.validateCloudSdk();
      sdk.validateAppEngineJavaComponents();
      page = new MavenAppEngineStandardWizardPage();
      archetypePage = new MavenAppEngineStandardArchetypeWizardPage();
      addPage(page);
      addPage(archetypePage);
    } catch (CloudSdkNotFoundException ex) {
      addPage(new CloudSdkMissingPage(AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_MAVEN));
    } catch (CloudSdkOutOfDateException ex) {
      addPage(new CloudSdkOutOfDatePage(
          AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_MAVEN));
    } catch (AppEngineJavaComponentsNotInstalledException ex) {
      addPage(new AppEngineJavaComponentMissingPage(
          AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_MAVEN));
    }
  }


  @Override
  public boolean performFinish() {
    AnalyticsPingManager.getInstance().sendPing(
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_COMPLETE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_MAVEN);

    if (cloudSdkLocation == null) {
      cloudSdkLocation = CloudSdkPrompter.getCloudSdkLocation(getShell());
      if (cloudSdkLocation == null) {
        return false;
      }
    }

    final CreateMavenBasedAppEngineStandardProject operation
        = new CreateMavenBasedAppEngineStandardProject();
    operation.setPackageName(page.getPackageName());
    operation.setGroupId(page.getGroupId());
    operation.setArtifactId(page.getArtifactId());
    operation.setVersion(page.getVersion());
    operation.setLocation(page.getLocationPath());
    operation.setArchetype(archetypePage.getArchetype());
    operation.setAppEngineLibraryIds(page.getSelectedLibraries());

    IRunnableWithProgress runnable = new IRunnableWithProgress() {
      @Override
      public void run(IProgressMonitor monitor)
          throws InvocationTargetException, InterruptedException {
        operation.run(monitor);
      }
    };

    try {
      boolean fork = true;
      boolean cancelable = true;
      getContainer().run(fork, cancelable, runnable);

      // open most important file created by wizard in editor
      IFile file = operation.getMostImportant();
      WorkbenchUtil.openInEditor(workbench, file);
      return true;
    } catch (InterruptedException ex) {
      return false;
    } catch (InvocationTargetException ex) {
      StatusUtil.setErrorStatus(this, Messages.getString("PROJECT_CREATION_FAILED"), ex.getCause());
      return false;
    }
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.workbench = workbench;
    if (cloudSdkLocation == null) {
      cloudSdkLocation = CloudSdkPrompter.getCloudSdkLocation(getShell());
      // if the user doesn't provide the Cloud SDK then we'll error in performFinish() too
    }
  }

}
