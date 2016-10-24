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
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineComponentPage;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.statushandlers.StatusManager;

import java.lang.reflect.InvocationTargetException;

public class MavenArchetypeProjectWizard extends Wizard implements INewWizard {
  private MavenAppEngineStandardWizardPage page;
  private MavenAppEngineStandardArchetypeWizardPage archetypePage;

  public MavenArchetypeProjectWizard() {
    setWindowTitle(Messages.getString("WIZARD_TITLE")); //$NON-NLS-1$
    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    if (appEngineJavaComponentExists()) {
      page = new MavenAppEngineStandardWizardPage();
      archetypePage = new MavenAppEngineStandardArchetypeWizardPage();
      this.addPage(page);
      this.addPage(archetypePage);
    } else {
      this.addPage(new AppEngineComponentPage(false /* forNativeProjectWizard */));
    }
  }

  @Override
  public boolean performFinish() {
    AnalyticsPingManager.getInstance().sendPing(
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_COMPLETE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_MAVEN);

    final CreateMavenBasedAppEngineStandardProject operation = new CreateMavenBasedAppEngineStandardProject();
    operation.setPackageName(page.getPackageName());
    operation.setGroupId(page.getGroupId());
    operation.setArtifactId(page.getArtifactId());
    operation.setVersion(page.getVersion());
    operation.setLocation(page.getLocationPath());
    operation.setArchetype(archetypePage.getArchetype());

    IRunnableWithProgress runnable = new IRunnableWithProgress() {
      @Override
      public void run(IProgressMonitor monitor)
          throws InvocationTargetException, InterruptedException {
        operation.run(monitor);
      }
    };

    IStatus status = Status.OK_STATUS;
    try {
      boolean fork = true;
      boolean cancelable = true;
      getContainer().run(fork, cancelable, runnable);
    } catch (InterruptedException ex) {
      status = Status.CANCEL_STATUS;
    } catch (InvocationTargetException ex) {
      status = new Status(Status.ERROR, getClass().getName(), 0, ex.getMessage(), ex.getCause());
      StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
    }

    return status.isOK();
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {}

  private boolean appEngineJavaComponentExists() {
    try {
      new CloudSdk.Builder().build().validateAppEngineJavaComponents();
      return true;
    } catch (AppEngineJavaComponentsNotInstalledException ex) {
      return false;
    }
  }
}
