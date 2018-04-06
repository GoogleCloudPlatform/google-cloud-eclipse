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

package com.google.cloud.tools.eclipse.appengine.deploy.ui.standard;

import com.google.cloud.tools.eclipse.appengine.deploy.DeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.StagingDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardStagingDelegate;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployCommandHandler;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.DeployPreferencesDialog;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.util.jdt.JreDetector;
import java.nio.file.Path;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public class StandardDeployCommandHandler extends DeployCommandHandler {

  public StandardDeployCommandHandler() {
    super(AnalyticsEvents.APP_ENGINE_DEPLOY_STANDARD);
  }

  @Override
  protected boolean checkProject(Shell shell, IProject project) throws CoreException {
    if (WebProjectUtil.hasJsps(project)) {
      IJavaProject javaProject = JavaCore.create(project);
      IVMInstall vmInstall = JavaRuntime.getVMInstall(javaProject);
      if (!JreDetector.isDevelopmentKit(vmInstall)) {
        return MessageDialog.openQuestion(
            shell,
            Messages.getString("vm.is.jre.title"),
            Messages.getString(
                "vm.is.jre.proceed",
                project.getName(),
                describeVm(vmInstall),
                vmInstall.getInstallLocation()));
      }
    }
    return true;
  }

  private String describeVm(IVMInstall vmInstall) {
    if (vmInstall instanceof IVMInstall2) {
      return vmInstall.getName() + " (" + ((IVMInstall2) vmInstall).getJavaVersion() + ")";
    }
    return vmInstall.getName();
  }

  @Override
  protected DeployPreferencesDialog newDeployPreferencesDialog(Shell shell, IProject project,
      IGoogleLoginService loginService, IGoogleApiFactory googleApiFactory) {
    String title = Messages.getString("deploy.preferences.dialog.title.standard");
    return new StandardDeployPreferencesDialog(
        shell, title, project, loginService, googleApiFactory);
  }

  @Override
  protected StagingDelegate getStagingDelegate(IProject project) throws CoreException {
    IJavaProject javaProject = JavaCore.create(project);
    IVMInstall vmInstall = JavaRuntime.getVMInstall(javaProject);
    Path javaHome = null;
    if (vmInstall != null) {
      javaHome = vmInstall.getInstallLocation().toPath();
    }
    return new StandardStagingDelegate(project, javaHome);
  }

  @Override
  protected DeployPreferences getDeployPreferences(IProject project) {
    return new DeployPreferences(project);
  }
}
