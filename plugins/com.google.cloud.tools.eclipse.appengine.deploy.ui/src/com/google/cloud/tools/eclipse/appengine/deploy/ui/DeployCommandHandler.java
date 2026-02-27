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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.deploy.CleanupOldDeploysJob;
import com.google.cloud.tools.eclipse.appengine.deploy.DeployJob;
import com.google.cloud.tools.eclipse.appengine.deploy.DeployPreferences;
import com.google.cloud.tools.eclipse.appengine.deploy.StagingDelegate;
import com.google.cloud.tools.eclipse.sdk.internal.CloudSdkPreferences;
import com.google.cloud.tools.eclipse.ui.util.MessageConsoleUtilities;
import com.google.cloud.tools.eclipse.ui.util.ProjectFromSelectionHelper;
import com.google.cloud.tools.eclipse.ui.util.ServiceUtils;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.console.ConsoleColorProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.osgi.framework.FrameworkUtil;

/**
 * Command handler to deploy a web application project to App Engine.
 * <p>
 * It copies the project's WAR or exploded WAR to a staging directory and then executes
 * the staging and deploy operations provided by the App Engine Plugins Core Library.
 */
public abstract class DeployCommandHandler extends AbstractHandler {

  private final String analyticsDeployEventMetadataKey;

  public DeployCommandHandler(String analyticsDeployEventMetadataKey) {
    this.analyticsDeployEventMetadataKey = analyticsDeployEventMetadataKey;
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    try {
      IProject project = getSelectedProject(event);

      if (PlatformUI.isWorkbenchRunning()) {
        if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
          return null;
        }
        if (getWorkspace(event).isAutoBuilding()) {
          Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
        }
      }
      Shell shell = HandlerUtil.getActiveShell(event);
      if (project != null && !checkProjectErrors(project)) {
        MessageDialog.openInformation(
            shell,
            Messages.getString("build.error.dialog.title"),
            Messages.getString("build.error.dialog.message"));
        return null;
      }
      if (!checkProject(shell, project)) {
        return null;
      }

      DeployPreferencesDialog dialog =
          newDeployPreferencesDialog(shell, project);
      if (dialog.open() == Window.OK) {
        launchDeployJob(project, dialog.getCredential());
      } 
      // return value must be null, reserved for future use
      return null;
    } catch (CoreException | IOException | InterruptedException exception) {
      throw new ExecutionException(
          Messages.getString("deploy.failed.error.message"), exception); //$NON-NLS-1$
    } catch (OperationCanceledException ex) {
      /* ignore */
      return null;
    }
  }

  /**
   * Check that the project is deployable.
   *
   * @return {@code true} if deployable
   * @throws CoreException on error
   */
  protected boolean checkProject(Shell shell, IProject project) throws CoreException {
    return true;
  }

  protected IProject getSelectedProject(ExecutionEvent event)
      throws ExecutionException, CoreException {
    IProject project = ProjectFromSelectionHelper.getFirstProject(event);
    if (project == null) {
      throw new NullPointerException("Deploy menu enabled for non-project resources");
    }
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    if (facetedProject == null) {
      throw new NullPointerException("Deploy menu enabled for non-faceted projects");
    }
    return project;
  }

  private static IWorkspace getWorkspace(ExecutionEvent event) {
    return ServiceUtils.getService(event, IWorkspace.class);
  }

  protected abstract DeployPreferencesDialog newDeployPreferencesDialog(Shell shell,
      IProject project);

  // It should better be named "getDeployPreferencesSnapshot" or something implying that. The
  // snapshot then should be propagated as a single source of truth for the entire duration of
  // the deploy job: https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2416
  protected abstract DeployPreferences getDeployPreferences(IProject project);

  private static boolean checkProjectErrors(IProject project) throws CoreException {
    int severity = project.findMaxProblemSeverity(
        IMarker.PROBLEM, true /* includeSubtypes */, IResource.DEPTH_INFINITE);
    return severity != IMarker.SEVERITY_ERROR;
  }

  private void launchDeployJob(IProject project, Credential credential)
      throws IOException, CoreException {

    sendAnalyticsPing(AnalyticsEvents.APP_ENGINE_DEPLOY);

    IPath workDirectory = createWorkDirectory();
    DeployPreferences deployPreferences = getDeployPreferences(project);

    DeployConsole messageConsole =
        MessageConsoleUtilities.createConsole(getConsoleName(deployPreferences.getProjectId()),
                                              new DeployConsole.Factory());
    IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
    consoleManager.showConsoleView(messageConsole);
    ConsoleColorProvider colorProvider = new ConsoleColorProvider();
    MessageConsoleStream outputStream = messageConsole.newMessageStream();
    MessageConsoleStream errorStream = messageConsole.newMessageStream();
    outputStream.setActivateOnWrite(true);
    errorStream.setActivateOnWrite(true);
    outputStream.setColor(colorProvider.getColor(IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM));
    errorStream.setColor(colorProvider.getColor(IDebugUIConstants.ID_STANDARD_ERROR_STREAM));

    StagingDelegate stagingDelegate = getStagingDelegate(project);

    DeployJob deploy = new DeployJob(deployPreferences, credential, workDirectory,
        outputStream, errorStream, stagingDelegate);
    messageConsole.setJob(deploy);
    deploy.addJobChangeListener(new JobChangeAdapter() {

      @Override
      public void done(IJobChangeEvent event) {
        if (event.getResult().isOK()) {
          sendAnalyticsPing(AnalyticsEvents.APP_ENGINE_DEPLOY_SUCCESS);
        }
        launchCleanupJob();
      }
    });
    deploy.schedule();
  }

  protected abstract StagingDelegate getStagingDelegate(IProject project) throws CoreException;

  private static String getConsoleName(String projectId) {
    Date now = new Date();
    String nowString = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                                                      DateFormat.MEDIUM,
                                                      Locale.getDefault())
                                 .format(now);
    return MessageFormat.format("{0} - {1} ({2})",
                                Messages.getString("deploy.console.name"),
                                projectId,
                                nowString);
  }

  private static IPath createWorkDirectory() throws IOException {
    String now = Long.toString(System.currentTimeMillis());
    IPath workDirectory = getTempDir().append(now);
    Files.createDirectories(workDirectory.toFile().toPath());
    return workDirectory;
  }

  private static void launchCleanupJob() {
    new CleanupOldDeploysJob(getTempDir()).schedule();
  }

  private static IPath getTempDir() {
    // DeployJob.class: create in the non-UI bundle.
    return Platform.getStateLocation(FrameworkUtil.getBundle(DeployJob.class))
        .append("tmp");
  }

  private void sendAnalyticsPing(String event) {
    String cloudSdkManagement = CloudSdkPreferences.isAutoManaging()
        ? AnalyticsEvents.AUTOMATIC_CLOUD_SDK
        : AnalyticsEvents.MANUAL_CLOUD_SDK;
    Map<String, String> metadata = ImmutableMap.of(
        AnalyticsEvents.CLOUD_SDK_MANAGEMENT, cloudSdkManagement,
        // For a historical reason, the key serves as an actual value for std vs. flex.
        analyticsDeployEventMetadataKey, "null");

    AnalyticsPingManager.getInstance().sendPing(event, metadata);
  }
}
