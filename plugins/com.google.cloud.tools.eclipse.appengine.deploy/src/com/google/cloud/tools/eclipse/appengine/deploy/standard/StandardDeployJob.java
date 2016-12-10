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
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.eclipse.appengine.deploy.AppEngineProjectDeployer;
import com.google.cloud.tools.eclipse.appengine.deploy.Messages;
import com.google.cloud.tools.eclipse.appengine.login.CredentialHelper;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
 * </ol>
 * It uses a work directory where it will create separate directories for the exploded WAR and the
 * staging results.
 */
public class StandardDeployJob extends WorkspaceJob {

  private static final String STAGING_DIRECTORY_NAME = "staging";
  private static final String EXPLODED_WAR_DIRECTORY_NAME = "exploded-war";
  private static final String CREDENTIAL_FILENAME = "gcloud-credentials.json";

  private static final Logger logger = Logger.getLogger(StandardDeployJob.class.getName());

  private final ExplodedWarPublisher exporter;
  private final StandardProjectStaging staging;
  private AppEngineProjectDeployer deployer;

  //temporary way of error handling, after #439 is fixed, it'll be cleaner
  private IStatus cloudSdkProcessStatus = Status.OK_STATUS;
  private Process process;

  private StandardDeployJobConfig config;

  StandardDeployJob(ExplodedWarPublisher exporter,
                           StandardProjectStaging staging,
                           AppEngineProjectDeployer deployer,
                           StandardDeployJobConfig config) {
    super(Messages.getString("deploy.standard.runnable.name")); //$NON-NLS-1$

    Preconditions.checkNotNull(deployer, "deployer is null");
    Preconditions.checkNotNull(exporter, "exporter is null");
    Preconditions.checkNotNull(staging, "staging is null");
    Preconditions.checkNotNull(config, "config is null");

    this.exporter = exporter;
    this.staging = staging;
    this.deployer = deployer;
    this.config = config;
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 100);
    Path credentialFile = null;
    try {
      IPath workDirectory = config.getWorkDirectory();
      IPath explodedWarDirectory = workDirectory.append(EXPLODED_WAR_DIRECTORY_NAME);
      IPath stagingDirectory = workDirectory.append(STAGING_DIRECTORY_NAME);
      credentialFile = workDirectory.append(CREDENTIAL_FILENAME).toFile().toPath();
      saveCredential(credentialFile, config.getCredential());
      CloudSdk cloudSdk = getCloudSdk(credentialFile);

      try {
        getJobManager().beginRule(config.getProject(), progress);
        exporter.publish(config.getProject(), explodedWarDirectory, progress.newChild(10));
        staging.stage(explodedWarDirectory, stagingDirectory, cloudSdk, progress.newChild(20));
      } finally {
        getJobManager().endRule(config.getProject());
      }

      if (!cloudSdkProcessStatus.isOK()) {
        if (cloudSdkProcessStatus == Status.CANCEL_STATUS) {
          return cloudSdkProcessStatus;
        }
        // temporary way of error handling, after #439 is fixed, it'll be cleaner
        return StatusUtil.error(getClass(), "Staging failed, check the error message in the Console View");
      }
      deployer.deploy(stagingDirectory, cloudSdk, config.getDeployConfiguration(), progress.newChild(70));
      if (!cloudSdkProcessStatus.isOK() && cloudSdkProcessStatus != Status.CANCEL_STATUS) {
        // temporary way of error handling, after #439 is fixed, it'll be cleaner
        return StatusUtil.error(getClass(), "Deploy failed, check the error message in the Console View");
      }

      return cloudSdkProcessStatus;
    } catch (IOException exception) {
      throw new CoreException(StatusUtil.error(getClass(),
                                               Messages.getString("save.credential.failed"),
                                               exception));
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

  private void saveCredential(Path destination, Credential credential) throws IOException {
    String jsonCredential = new CredentialHelper().toJson(credential);
    Files.write(destination, jsonCredential.getBytes(StandardCharsets.UTF_8));
  }

  private CloudSdk getCloudSdk(Path credentialFile) {
    CloudSdk cloudSdk = new CloudSdk.Builder()
                          .addStdOutLineListener(config.getStdoutLineListener())
                          .addStdErrLineListener(config.getStderrLineListener())
                          .appCommandCredentialFile(credentialFile.toFile())
                          .startListener(new StoreProcessObjectListener())
                          .exitListener(new RecordProcessError())
                          .appCommandMetricsEnvironment(CloudToolsInfo.METRICS_NAME)
                          .appCommandMetricsEnvironmentVersion(CloudToolsInfo.getToolsVersion())
                          .build();
    return cloudSdk;
  }

  private final class StoreProcessObjectListener implements ProcessStartListener {
    @Override
    public void onStart(Process proces) {
      process = proces;
    }
  }

  private final class RecordProcessError implements ProcessExitListener {
    // temporary way of error handling, after #439 is fixed, it'll be cleaner
    @Override
    public void onExit(int exitCode) {
      // if it's cancelled we don't need to record the exit code from the process, it would be the exit code
      // that corresponds to the process.destroy()
      if (cloudSdkProcessStatus != Status.CANCEL_STATUS && exitCode != 0) {
        cloudSdkProcessStatus = StatusUtil.error(this, Messages.getString("cloudsdk.process.failed", exitCode));
      }
    }
  }

  public static class Builder {
    private ExplodedWarPublisher exporter;
    private StandardProjectStaging staging;
    private AppEngineProjectDeployer deployer;
    private StandardDeployJobConfig config;

    public Builder exporter(ExplodedWarPublisher exporter) {
      this.exporter = exporter;
      return this;
    }

    public Builder staging(StandardProjectStaging staging) {
      this.staging = staging;
      return this;
    }

    public Builder deployer(AppEngineProjectDeployer deployer) {
      this.deployer = deployer;
      return this;
    }

    public Builder config(StandardDeployJobConfig config) {
      this.config = config;
      return this;
    }

    public StandardDeployJob build() {
      Preconditions.checkNotNull(config, "config is null");

      if (exporter == null) {
        exporter = new ExplodedWarPublisher();
      }

      if (staging == null) {
        staging = new StandardProjectStaging();
      }

      if (deployer == null) {
        deployer = new AppEngineProjectDeployer();
      }

      return new StandardDeployJob(exporter, staging, deployer, config);
    }

  }
}
