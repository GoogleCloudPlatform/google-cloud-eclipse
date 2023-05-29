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

package com.google.cloud.tools.eclipse.appengine.deploy.util;

import com.google.cloud.tools.appengine.operations.Deployment;
import com.google.cloud.tools.appengine.operations.AppCfg;
import com.google.cloud.tools.appengine.operations.AppEngineWebXmlProjectStaging;
import com.google.cloud.tools.appengine.operations.CloudSdk;
import com.google.cloud.tools.appengine.operations.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.operations.Gcloud;
import com.google.cloud.tools.appengine.operations.cloudsdk.process.LegacyProcessHandler;
import com.google.cloud.tools.appengine.operations.cloudsdk.process.ProcessHandler;
import com.google.cloud.tools.appengine.operations.cloudsdk.process.ProcessHandlerException;
import com.google.cloud.tools.appengine.operations.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.appengine.operations.cloudsdk.process.StringBuilderProcessOutputLineListener;
import com.google.cloud.tools.eclipse.appengine.deploy.AppEngineProjectDeployer;
import com.google.cloud.tools.eclipse.appengine.deploy.Messages;
import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardStagingDelegate;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.sdk.GcloudStructuredLogErrorMessageCollector;
import com.google.cloud.tools.eclipse.sdk.MessageConsoleWriterListener;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * Helper class wrapping a process created by {@link CloudSdk} to hide the bulk of low-level work
 * dealing with process cancellation, process exit monitoring, error output line collection,
 * standard output collection, etc. Intended to be used exclusively by {@link
 * StandardStagingDelegate} and {@link AppEngineProjectDeployer} for their convenience.
 */
public class CloudSdkProcessWrapper {

  private Process process;
  private boolean interrupted;
  private IStatus exitStatus = Status.OK_STATUS;
  private ProcessOutputLineListener stdOutCaptor;
  private boolean initialized = false;

  /**
   * Collects messages of any gcloud structure log lines whose severity is ERROR. Note that the
   * collector is not used for staging, as the staging does not invoke gcloud.
   */
  private GcloudStructuredLogErrorMessageCollector gcloudErrorMessageCollector;

  /**
   * Sets up a {@link CloudSdk} to be used for App Engine deploy.
   */
  public Deployment getAppEngineDeployment(MessageConsoleStream normalOutputStream) 
      throws CloudSdkNotFoundException {
    Preconditions.checkState(GoogleApiFactory.INSTANCE.getCredential().isPresent(), "credential required for deploying");
    Preconditions.checkState(!initialized, "process wrapper already set up");
    initialized = true;

    CloudSdk cloudSdk = new CloudSdk.Builder().build();
    
    Gcloud gcloud = Gcloud.builder(cloudSdk)
        .setMetricsEnvironment(CloudToolsInfo.METRICS_NAME, CloudToolsInfo.getToolsVersion())
        .setShowStructuredLogs("always")  // turns on gcloud structured log
        .setOutputFormat("json")  // Deploy result will be in JSON.
        .build();

    // Gcloud sends structured deploy result (in JSON format) to stdout, so prepare to capture that.
    stdOutCaptor = StringBuilderProcessOutputLineListener.newListenerWithNewlines();
    // Gcloud sends structured gcloud logs (in JSON format) to stderr, so prepare to capture them.
    gcloudErrorMessageCollector = new GcloudStructuredLogErrorMessageCollector();

    ProcessHandler processHandler = LegacyProcessHandler.builder()
        .setStartListener(this::storeProcessObject)
        .setExitListener(this::recordProcessExitCode)
        // Gcloud sends normal operation output to stderr.
        .addStdErrLineListener(new MessageConsoleWriterListener(normalOutputStream))
        .addStdErrLineListener(gcloudErrorMessageCollector)
        .addStdOutLineListener(stdOutCaptor)
        .build();

    return gcloud.newDeployment(processHandler);
  }

  /**
   * Sets up a {@link CloudSdk} to be used for App Engine standard staging.
   *
   * @param javaHome JDK/JRE to 1) run {@code com.google.appengine.tools.admin.AppCfg} from
   *     {@code appengine-tools-api.jar}; and 2) compile JSPs during staging
   */
  public AppEngineWebXmlProjectStaging getAppEngineStandardStaging(Path javaHome,
      MessageConsoleStream stdoutOutputStream, MessageConsoleStream stderrOutputStream) 
          throws CloudSdkNotFoundException {
    Preconditions.checkState(!initialized, "process wrapper already set up");
    initialized = true;

    CloudSdk cloudSdk = javaHome == null
        ? new CloudSdk.Builder().build()
        : new CloudSdk.Builder().javaHome(javaHome).build();

    ProcessHandler processHandler = LegacyProcessHandler.builder()
        .setStartListener(this::storeProcessObject)
        .setExitListener(this::recordProcessExitCode)
        .addStdOutLineListener(new MessageConsoleWriterListener(stdoutOutputStream))
        .addStdErrLineListener(new MessageConsoleWriterListener(stderrOutputStream))
        .build();

    return AppCfg.builder(cloudSdk).build().newStaging(processHandler);
  }
  
  public String getDefaultProject() throws CloudSdkNotFoundException, IOException, ProcessHandlerException {
    CloudSdk cloudSdk = new CloudSdk.Builder().build();
    
    Gcloud gcloud = Gcloud.builder(cloudSdk)
      .setMetricsEnvironment(CloudToolsInfo.METRICS_NAME, CloudToolsInfo.getToolsVersion())
      .setShowStructuredLogs("always")  // turns on gcloud structured log
      .setOutputFormat("json")  // Deploy result will be in JSON.
      .build();
    
    return gcloud.runCommand(Arrays.asList("config", "get", "project"));
  }

  public void interrupt() {
    synchronized (this) {
      interrupted = true;  // not to miss destruction due to race condition
      if (process != null) {
        process.destroy();
      }
    }
  }

  public IStatus getExitStatus() {
    return exitStatus;
  }

  public String getStdOutAsString() {
    Preconditions.checkNotNull(stdOutCaptor);
    return stdOutCaptor.toString();
  }

  private void storeProcessObject(Process process) {
    synchronized (this) {
      this.process = process;
      if (interrupted) {
        process.destroy();
      }
    }
  }

  @VisibleForTesting
  void recordProcessExitCode(int exitCode) {
    if (exitCode != 0) {
      exitStatus = StatusUtil.error(this, getErrorMessage(exitCode), exitCode);
    } else {
      exitStatus = Status.OK_STATUS;
    }
  }

  private String getErrorMessage(int exitCode) {
    if (gcloudErrorMessageCollector != null) {
      List<String> lines = gcloudErrorMessageCollector.getErrorMessages();
      if (!lines.isEmpty()) {
        return Joiner.on('\n').join(lines);
      }
    }
    return Messages.getString("cloudsdk.process.failed", exitCode);
  }
}
