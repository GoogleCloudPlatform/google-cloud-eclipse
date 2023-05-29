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
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.WebProjectUtil;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.util.jdt.JreDetector;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class StandardDeployCommandHandler extends DeployCommandHandler {
  private static final Logger logger = Logger.getLogger(StandardDeployCommandHandler.class.getName());

  public StandardDeployCommandHandler() {
    super(AnalyticsEvents.APP_ENGINE_DEPLOY_STANDARD);
  }

  @Override
  protected boolean checkProject(Shell shell, IProject project) throws CoreException {
    return checkJspConfiguration(shell, project)
        && checkAppEngineRuntimeCompatibility(shell, project);
  }

  private boolean checkAppEngineRuntimeCompatibility(Shell shell, IProject project) {
    try {
      IFacetedProject facetedProject = ProjectFacetsManager.create(project);
      if (facetedProject == null) {
        return false;
      }

      if (AppEngineStandardFacet.usesObsoleteRuntime(facetedProject)) {
        String message = Messages.getString("obsolete.runtime.message", project.getName());
        MessageDialog.openError(shell, Messages.getString("obsolete.runtime.title"), message);
        return false;
      }
    } catch(CoreException ex) {
      logger.log(Level.WARNING, "Unable to check project use of obsolete App Engine runtime", ex);
    }
    return true;
  }

  protected boolean checkJspConfiguration(Shell shell, IProject project) throws CoreException {
    // If we have JSPs, ensure the project is configured with a JDK: required by staging
    // which precompiles the JSPs.  We could try to find a compatible JDK, but there's
    // a possibility that we select an inappropriate one and introduce problems.
    if (!WebProjectUtil.hasJsps(project)) {
      return true;
    }

    IJavaProject javaProject = JavaCore.create(project);
    IVMInstall vmInstall = JavaRuntime.getVMInstall(javaProject);
    if (JreDetector.isDevelopmentKit(vmInstall)) {
      return true;
    }

    String title = Messages.getString("vm.is.jre.title");
    String message =
        Messages.getString(
            "vm.is.jre.proceed",
            project.getName(),
            describeVm(vmInstall),
            vmInstall.getInstallLocation());
    String[] buttonLabels =
        new String[] {Messages.getString("deploy.button"), IDialogConstants.CANCEL_LABEL};
    MessageDialog dialog =
        new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, 0, buttonLabels);
    return dialog.open() == 0;
  }

  private String describeVm(IVMInstall vmInstall) {
    if (vmInstall instanceof IVMInstall2) {
      return vmInstall.getName() + " (" + ((IVMInstall2) vmInstall).getJavaVersion() + ")";
    }
    return vmInstall.getName();
  }

  @Override
  protected DeployPreferencesDialog newDeployPreferencesDialog(
      Shell shell,
      IProject project) {
    String title = Messages.getString("deploy.preferences.dialog.title.standard");
    return new StandardDeployPreferencesDialog(
        shell, title, project);
  }

  @Override
  protected StagingDelegate getStagingDelegate(IProject project) {
    Path javaHome = null;
    try {
      IJavaProject javaProject = JavaCore.create(project);
      IVMInstall vmInstall = JavaRuntime.getVMInstall(javaProject);
      if (vmInstall != null) {
        javaHome = vmInstall.getInstallLocation().toPath();
      }
    } catch (CoreException ex) {
      // Give up.
    }
    return new StandardStagingDelegate(project, javaHome);
  }

  @Override
  protected DeployPreferences getDeployPreferences(IProject project) {
    return new DeployPreferences(project);
  }
}
