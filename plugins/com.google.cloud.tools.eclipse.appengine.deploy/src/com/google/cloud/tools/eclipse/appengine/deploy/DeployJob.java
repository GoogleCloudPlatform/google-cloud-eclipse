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

package com.google.cloud.tools.eclipse.appengine.deploy;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.eclipse.login.CredentialHelper;
import com.google.cloud.tools.eclipse.sdk.CloudSdkProcessFacade;
import com.google.cloud.tools.eclipse.sdk.ui.MessageConsoleWriterOutputLineListener;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * Executes a job that deploys a project to App Engine standard or flexible environment.
 * <p>
 * Deploy steps:
 * <ol>
 *  <li>prepare deploy artifact (WAR or exploded WAR)</li>
 *  <li>stage project for deploy</li>
 *  <li>deploy staged project</li>
 *  <li>launch the deployed app in browser</li>
 * </ol>
 * It uses a work directory where it will create, e.g., a JSON user credential file, a WAR, a
 * directory to put exploded WAR contents, a directory to put staging results, etc.
 */
public class DeployJob extends WorkspaceJob {

  private static final String STAGING_DIRECTORY_NAME = "staging";
  private static final String SAFE_STAGING_WORK_DIRECTORY_NAME = "staging-work";
  private static final String CREDENTIAL_FILENAME = "gcloud-credentials.json";
  private static final String DEFAULT_SERVICE = "default";

  private final IProject project;
  private final Credential credential;
  private final IPath workDirectory;
  private final ProcessOutputLineListener stagingStdoutLineListener;
  private final ProcessOutputLineListener stderrLineListener;
  private final DeployPreferences deployPreferences;
  private final StagingDelegate stager;

  private CloudSdkProcessFacade deployCloudSdkFacade;
  private CloudSdkProcessFacade stagingCloudSdkFacade;
  private boolean canceled;

  /**
   * @param workDirectory temporary work directory the job can safely use (e.g., for creating and
   *     copying various files to stage and deploy)
   * @param stagingOutputStream {@link MessageConsoleStream} to stream the staging operation stdout
   *     (where {@code appcfg.sh} outputs user-visible log messages)
   * @param stderrOutputStream {@link MessageConsoleStream} to stream the deploy operation stderr
   *     (where {@code gcloud app deploy} outputs user-visible log messages) and the staging
   *     operation stderr
   */
  public DeployJob(IProject project, Credential credential, IPath workDirectory,
      MessageConsoleStream stagingOutputStream, MessageConsoleStream stderrOutputStream,
      StagingDelegate stager) {
    super(Messages.getString("deploy.job.name")); //$NON-NLS-1$
    deployPreferences = new DeployPreferences(project);
    Preconditions.checkNotNull(deployPreferences.getProjectId());
    Preconditions.checkArgument(!deployPreferences.getProjectId().isEmpty());
    this.project = project;
    this.credential = credential;
    this.workDirectory = workDirectory;
    stagingStdoutLineListener = new MessageConsoleWriterOutputLineListener(stagingOutputStream);
    stderrLineListener = new MessageConsoleWriterOutputLineListener(stderrOutputStream);
    this.stager = stager;
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 100);

    try {
      IPath stagingDirectory = workDirectory.append(STAGING_DIRECTORY_NAME);
      Path credentialFile = workDirectory.append(CREDENTIAL_FILENAME).toFile().toPath();

      IStatus saveStatus = saveCredential(credentialFile);
      if (saveStatus != Status.OK_STATUS) {
        return saveStatus;
      }

      synchronized (this) {
        if (canceled) {
          return Status.CANCEL_STATUS;
        }
        deployCloudSdkFacade = CloudSdkProcessFacade.forDeploy(credentialFile, stderrLineListener);
        stagingCloudSdkFacade = CloudSdkProcessFacade.forStandardStaging(getProjectVm(project),
            stagingStdoutLineListener, stderrLineListener);
      }

      IStatus stagingStatus = stageProject(stagingDirectory, progress.newChild(30));
      if (stagingStatus != Status.OK_STATUS) {
        return stagingStatus;
      } else if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }

      IStatus deployStatus = deployProject(stagingDirectory, progress.newChild(70));
      if (deployStatus != Status.OK_STATUS) {
        return deployStatus;
      } else if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }

      return openAppInBrowser();
    } finally {
      progress.done();
    }
  }

  private static Path getProjectVm(IProject project) throws CoreException {
    IJavaProject javaProject = JavaCore.create(project);
    IVMInstall vmInstall = JavaRuntime.getVMInstall(javaProject);
    if (vmInstall != null) {
      return vmInstall.getInstallLocation().toPath();
    }
    return null;
  }

  @Override
  protected void canceling() {
    synchronized (this) {
      canceled = true;
      if (deployCloudSdkFacade != null) {
        deployCloudSdkFacade.cancel();
      }
      if (stagingCloudSdkFacade != null) {
        stagingCloudSdkFacade.cancel();
      }
    }
    super.canceling();
  }

  private IStatus saveCredential(Path destination) {
    try {
      CredentialHelper.toJsonFile(credential, destination);
      return Status.OK_STATUS;
    } catch (IOException ex) {
      return StatusUtil.error(this, Messages.getString("save.credential.failed"), ex);
    }
  }

  private IStatus stageProject(IPath stagingDirectory, IProgressMonitor monitor) {
    SubMonitor progress = SubMonitor.convert(monitor, 100);

    try {
      getJobManager().beginRule(project, progress.newChild(1));
      IPath safeWorkDirectory = workDirectory.append(SAFE_STAGING_WORK_DIRECTORY_NAME);
      IStatus status = stager.stage(project, stagingDirectory, safeWorkDirectory,
          stagingCloudSdkFacade.getCloudSdk(), progress.newChild(99));
      if (stagingCloudSdkFacade.getExitStatus() != Status.OK_STATUS) {
        return stagingCloudSdkFacade.getExitStatus();
      }
      return status;
    } catch (IllegalArgumentException ex) {
      return StatusUtil.error(this, Messages.getString("deploy.job.staging.failed"), ex);
    } finally {
      getJobManager().endRule(project);
    }
  }

  private IStatus deployProject(IPath stagingDirectory, IProgressMonitor monitor) {
    IPath optionalConfigurationFilesDirectory = null;
    if (deployPreferences.isIncludeOptionalConfigurationFiles()) {
      optionalConfigurationFilesDirectory = stager.getOptionalConfigurationFilesDirectory();
    }

    new AppEngineProjectDeployer().deploy(stagingDirectory, deployCloudSdkFacade.getCloudSdk(),
        deployPreferences, optionalConfigurationFilesDirectory, monitor);
    return deployCloudSdkFacade.getExitStatus();
  }

  private IStatus openAppInBrowser() {
    try {
      String rawDeployOutput = deployCloudSdkFacade.getStdOutAsString();
      AppEngineDeployOutput structuredOutput = AppEngineDeployOutput.parse(rawDeployOutput);

      boolean promoted = deployPreferences.isAutoPromote();
      String appLocation = getDeployedAppUrl(promoted, structuredOutput);
      String project = deployPreferences.getProjectId();
      String browserTitle = Messages.getString("browser.launch.title", project);
      WorkbenchUtil.openInBrowserInUiThread(appLocation, null, browserTitle, browserTitle);
      return Status.OK_STATUS;
    } catch (IndexOutOfBoundsException | JsonParseException ex)  {
      return StatusUtil.error(this, Messages.getString("browser.launch.failed"), ex);
    }
  }

  @VisibleForTesting
  static String getDeployedAppUrl(boolean promoted, AppEngineDeployOutput deployOutput) {
    String version = deployOutput.getVersion();
    String service = deployOutput.getService();
    String projectId = deployOutput.getProject();
    boolean usingDefaultService = DEFAULT_SERVICE.equals(service);

    String domain = ".appspot.com";
    int colon = projectId.indexOf(':');
    if (colon >= 0) {
      domain = ".googleplex.com";
      projectId = projectId.substring(colon + 1);
    }

    if (promoted && usingDefaultService) {
      return "https://" + projectId + domain;
    } else if (promoted && !usingDefaultService) {
      return "https://" + service +  "-dot-"+  projectId + domain;
    } else if (!promoted && usingDefaultService) {
      return "https://" + version + "-dot-" + projectId + domain;
    } else {
      return "https://" + version + "-dot-" + service +  "-dot-"+  projectId + domain;
    }
  }

}
