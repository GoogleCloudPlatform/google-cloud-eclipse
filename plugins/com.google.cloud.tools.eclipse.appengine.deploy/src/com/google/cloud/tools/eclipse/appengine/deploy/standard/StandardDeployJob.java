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

package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.appengine.api.deploy.DefaultDeployConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.eclipse.appengine.deploy.AppEngineDeployOutput;
import com.google.cloud.tools.eclipse.appengine.deploy.AppEngineProjectDeployer;
import com.google.cloud.tools.eclipse.appengine.deploy.Messages;
import com.google.cloud.tools.eclipse.login.CredentialHelper;
import com.google.cloud.tools.eclipse.sdk.CollectingLineListener;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Executes a job that deploys a project to App Engine Standard.
 * <p>
 * Deploy steps:
 * <ol>
 *  <li>export exploded WAR</li>
 *  <li>stage project for deploy</li>
 *  <li>deploy staged project</li>
 *  <li>launch the deployed app in browser</li>
 * </ol>
 * It uses a work directory where it will create separate directories for the exploded WAR and the
 * staging results.
 */
public class StandardDeployJob extends WorkspaceJob {

  private static final String STAGING_DIRECTORY_NAME = "staging";
  private static final String EXPLODED_WAR_DIRECTORY_NAME = "exploded-war";
  private static final String CREDENTIAL_FILENAME = "gcloud-credentials.json";
  private static final String ERROR_MESSAGE_PREFIX = "ERROR:";
  private static final String DEFAULT_SERVICE = "default";

  private static final Logger logger = Logger.getLogger(StandardDeployJob.class.getName());

  //temporary way of error handling, after #439 is fixed, it'll be cleaner
  private IStatus cloudSdkProcessStatus = Status.OK_STATUS;
  private Process process;

  private final IProject project;
  private final Credential credential;
  private final IPath workDirectoryParent;
  private final ProcessOutputLineListener stagingStdoutLineListener;
  private final ProcessOutputLineListener deployStdoutLineListener;
  private final ProcessOutputLineListener stderrLineListener;
  private final DefaultDeployConfiguration deployConfiguration;
  private final boolean includeOptionalConfigurationFiles;
  private final CollectingLineListener errorCollectingLineListener;

  public StandardDeployJob(IProject project,
                           Credential credential,
                           IPath workDirectoryParent,
                           ProcessOutputLineListener stagingStdoutLineListener,
                           ProcessOutputLineListener stderrLineListener,
                           DefaultDeployConfiguration deployConfiguration,
                           boolean includeOptionalConfigurationFiles) {
    super(Messages.getString("deploy.standard.runnable.name")); //$NON-NLS-1$
    this.project = project;
    this.credential = credential;
    this.workDirectoryParent = workDirectoryParent;
    this.stagingStdoutLineListener = stagingStdoutLineListener;
    this.stderrLineListener = stderrLineListener;
    this.deployConfiguration = deployConfiguration;
    this.includeOptionalConfigurationFiles = includeOptionalConfigurationFiles;
    // TODO: change to StringBuilderProcessOutputLineListener from the appengine-plugins-core
    deployStdoutLineListener = new StringBuilderProcessOutputLineListener();
    errorCollectingLineListener =
        new CollectingLineListener(new Predicate<String>() {
                                     @Override
                                     public boolean apply(String line) {
                                       return line != null
                                           && line.startsWith(ERROR_MESSAGE_PREFIX);
                                     }
                                   });
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 100);
    Path credentialFile = null;

    try {
      IPath workDirectory = workDirectoryParent;
      IPath explodedWarDirectory = workDirectory.append(EXPLODED_WAR_DIRECTORY_NAME);
      IPath stagingDirectory = workDirectory.append(STAGING_DIRECTORY_NAME);
      credentialFile = workDirectory.append(CREDENTIAL_FILENAME).toFile().toPath();

      IStatus saveStatus = saveCredential(credentialFile, credential);
      if (saveStatus != Status.OK_STATUS) {
        return saveStatus;
      }

      IStatus stagingStatus = stageProject(
          credentialFile, explodedWarDirectory, stagingDirectory, progress.newChild(30));
      if (stagingStatus != Status.OK_STATUS) {
        return stagingStatus;
      }

      IStatus deployStatus = deployProject(credentialFile, stagingDirectory, progress.newChild(70));
      if (deployStatus != Status.OK_STATUS) {
        return deployStatus;
      }

      return openAppInBrowser();
    } finally {
      if (credentialFile != null) {
        try {
          Files.delete(credentialFile);
        } catch (IOException exception) {
          logger.log(Level.WARNING, "Could not delete credential file after deploy", exception);
        }
      }
      monitor.done();
    }
  }

  @Override
  protected void canceling() {
    cloudSdkProcessStatus = Status.CANCEL_STATUS;
    if (process != null) {
      process.destroy();
    }
    super.canceling();
  }

  private IStatus saveCredential(Path destination, Credential credential) {
    String jsonCredential = new CredentialHelper().toJson(credential);
    try {
      Files.write(destination, jsonCredential.getBytes(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      return StatusUtil.error(getClass(), Messages.getString("save.credential.failed"), ex);
    }
    return Status.OK_STATUS;
  }

  private IStatus stageProject(Path credentialFile,
      IPath explodedWarDirectory, IPath stagingDirectory, IProgressMonitor monitor) {
    SubMonitor progress = SubMonitor.convert(monitor, 100);
    RecordProcessError stagingExitListener = new RecordProcessError();
    CloudSdk cloudSdk = getCloudSdk(credentialFile, stagingStdoutLineListener, stagingExitListener);

    try {
      getJobManager().beginRule(project, progress);
      new ExplodedWarPublisher().publish(project, explodedWarDirectory, progress.newChild(40));
      new StandardProjectStaging().stage(explodedWarDirectory, stagingDirectory,
          cloudSdk, progress.newChild(60));
      return stagingExitListener.getExitStatus();
    } catch (CoreException | IllegalArgumentException | OperationCanceledException ex) {
      return StatusUtil.error(getClass(), Messages.getString("deploy.job.staging.failed"), ex);
    } finally {
      getJobManager().endRule(project);
    }
  }

  private IStatus deployProject(Path credentialFile, IPath stagingDirectory,
      IProgressMonitor monitor) {
    RecordProcessError deployExitListener = new RecordProcessError();
    CloudSdk cloudSdk = getCloudSdk(credentialFile, deployStdoutLineListener, deployExitListener);
    new AppEngineProjectDeployer().deploy(stagingDirectory, cloudSdk, deployConfiguration,
        includeOptionalConfigurationFiles, monitor);
    return deployExitListener.getExitStatus();
  }

  private CloudSdk getCloudSdk(Path credentialFile,
      ProcessOutputLineListener stdoutLineListener, ProcessExitListener processExitListener) {
    CloudSdk cloudSdk = new CloudSdk.Builder()
                          .addStdOutLineListener(stdoutLineListener)
                          .addStdErrLineListener(stderrLineListener)
                          .addStdErrLineListener(errorCollectingLineListener)
                          .appCommandCredentialFile(credentialFile.toFile())
                          .startListener(new StoreProcessObjectListener())
                          .exitListener(processExitListener)
                          .appCommandMetricsEnvironment(CloudToolsInfo.METRICS_NAME)
                          .appCommandMetricsEnvironmentVersion(CloudToolsInfo.getToolsVersion())
                          .appCommandOutputFormat("json")
                          .build();
    return cloudSdk;
  }

  private IStatus openAppInBrowser() {
    AppEngineDeployOutput deployOutput = null;

    try {
      deployOutput = getDeployOutput();
      String appLocation = getDeployedAppUrl(deployOutput);
      String project = deployConfiguration.getProject();
      String browserTitle = Messages.getString("browser.launch.title", project);
      WorkbenchUtil.openInBrowserInUiThread(appLocation, null, browserTitle, browserTitle);
      return Status.OK_STATUS;
    } catch (CoreException ex) {
      return StatusUtil.error(getClass(), Messages.getString("browser.launch.failed"), ex);
    }
  }

  /**
   * @return the error message obtained from <code>errorCollectingLineListener()</code> or
   * <code>defaultMessage</code>
   */
  private String getErrorMessageOrDefault(String defaultMessage) {
    // TODO: Check the assumption that if there are error messages during staging collected via
    // the errorCollectingLineListener, the staging process will have a non-zero exitcode,
    // making it ok to use the same errorCollectingLineListener for the deploy process
    List<String> messages = errorCollectingLineListener.getCollectedMessages();
    if (!messages.isEmpty()) {
      return Joiner.on('\n').join(messages);
    } else {
      return defaultMessage;
    }
  }

  private AppEngineDeployOutput getDeployOutput() throws CoreException {
    try {
      String rawDeployOutput = deployStdoutLineListener.toString();
      return AppEngineDeployOutput.parse(rawDeployOutput);
    } catch (IndexOutOfBoundsException | JsonParseException ex)  {
      throw new CoreException(StatusUtil.error(getClass(), "Error parsing deploy output", ex));
    }
  }

  @VisibleForTesting
  public String getDeployedAppUrl(AppEngineDeployOutput deployOutput) {
    boolean promoting = deployConfiguration.getPromote();
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
    
    if (promoting && usingDefaultService) {
      return "https://" + projectId + domain;
    } else if (promoting && !usingDefaultService) {
      return "https://" + service +  "-dot-"+  projectId + domain;
    } else if (!promoting && usingDefaultService) {
      return "https://" + version + "-dot-" + projectId + domain;
    } else {
      return "https://" + version + "-dot-" + service +  "-dot-"+  projectId + domain;
    }
  }

  private final class StoreProcessObjectListener implements ProcessStartListener {
    @Override
    public void onStart(Process proces) {
      process = proces;
    }
  }

  private class RecordProcessError implements ProcessExitListener {
    private IStatus status;

    @Override
    public void onExit(int exitCode) {
      if (cloudSdkProcessStatus == Status.CANCEL_STATUS) {
        status = cloudSdkProcessStatus;
      } else if (exitCode != 0) {
        // temporary way of error handling, after #439 is fixed, it'll be cleaner
        String errorMessage =
            getErrorMessageOrDefault(Messages.getString("cloudsdk.process.failed", exitCode));
        status = StatusUtil.error(this, errorMessage);
      } else {
        status = Status.OK_STATUS;
      }
    }

    /**
     * @return the status on exit of the process or null if the process has not exited.
     */
    public IStatus getExitStatus() {
      return status;
    }
  }

  private static class StringBuilderProcessOutputLineListener implements ProcessOutputLineListener {
    private final StringBuffer buffer = new StringBuffer();

    public StringBuilderProcessOutputLineListener() {
    }

    @Override
    public void onOutputLine(String line) {
      buffer.append(line);
    }

    @Override
    public String toString() {
      return buffer.toString();
    }
  }

}