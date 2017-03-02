/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.newproject.standard;

import com.google.cloud.tools.eclipse.appengine.libraries.ILibraryClasspathContainerResolverService;
import com.google.cloud.tools.eclipse.appengine.libraries.ILibraryClasspathContainerResolverService.AppEngineRuntime;
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectConfig;
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectWizard;
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineWizardPage;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProject;
import com.google.cloud.tools.eclipse.appengine.newproject.Messages;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.lang.reflect.InvocationTargetException;
import javax.inject.Inject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class AppEngineStandardProjectWizard extends AppEngineProjectWizard {

  @Inject
  private ILibraryClasspathContainerResolverService resolverService;

  public AppEngineStandardProjectWizard(){
    setWindowTitle(Messages.getString("new.app.engine.standard.project"));
  }

  @Override
  public AppEngineWizardPage createWizardPage() {
    return new AppEngineStandardWizardPage();
  }

  @Override
  public void sendAnalyticsPing() {
    AnalyticsPingManager.getInstance().sendPing(
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_COMPLETE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE,
        AnalyticsEvents.APP_ENGINE_NEW_PROJECT_WIZARD_TYPE_NATIVE);
  }

  @Override
  public IStatus validateDependencies(boolean fork, boolean cancelable) {
    IStatus status = Status.OK_STATUS;
    try {
      DependencyValidator dependencyValidator = new DependencyValidator();
      getContainer().run(fork, cancelable, dependencyValidator);
      if (!dependencyValidator.result.isOK()) {
        status = StatusUtil.setErrorStatus(this,
                                           Messages.getString("project.creation.failed"),
                                           dependencyValidator.result);
      }
    } catch (InvocationTargetException ex) {
      status = StatusUtil.setErrorStatus(this, Messages.getString("project.creation.failed"), ex.getCause());
    } catch (InterruptedException e) {
      status = Status.CANCEL_STATUS;
    }
    return status;
  }
  
  
  private class DependencyValidator implements IRunnableWithProgress {

    private IStatus result = null;

    @Override
    public void run(IProgressMonitor monitor)
        throws InvocationTargetException, InterruptedException {
      result = resolverService.checkRuntimeAvailability(AppEngineRuntime.STANDARD_JAVA_7, monitor);
    }
  }


  @Override
  public CreateAppEngineWtpProject getAppEngineProjectCreationOperation(AppEngineProjectConfig config,
      IAdaptable uiInfoAdapter) {
    return new CreateAppEngineStandardWtpProject(config, uiInfoAdapter);
  }

}
