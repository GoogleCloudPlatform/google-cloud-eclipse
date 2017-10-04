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

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.appengine.cloudsdk.process.StringBuilderProcessOutputLineListener;
import com.google.cloud.tools.eclipse.appengine.deploy.AppEngineProjectDeployer;
import com.google.cloud.tools.eclipse.appengine.deploy.Messages;
import com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardStagingDelegate;
import com.google.cloud.tools.eclipse.sdk.CollectingLineListener;
import com.google.cloud.tools.eclipse.sdk.MessageConsoleWriterOutputLineListener;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * Helper class wrapping {@link CloudSdk} to hide the bulk of low-level work dealing with process
 * cancellation, process exit monitoring, error output line collection, standard output collection,
 * etc. Intended to be used exclusively by {@link StandardStagingDelegate} and
 * {@link AppEngineProjectDeployer} for their convenience.
 */
public class CloudSdkProcessWrapper {

  private static final String ERROR_MESSAGE_PREFIX = "ERROR:";
  private static final Predicate<String> IS_ERROR_LINE = new Predicate<String>() {
    @Override
    public boolean apply(String line) {
      return line != null && line.startsWith(ERROR_MESSAGE_PREFIX);
    }
  };

  private CloudSdk cloudSdk;

  private Process process;
  private boolean interrupted;
  private IStatus exitStatus = Status.OK_STATUS;
  private ProcessOutputLineListener stdOutCaptor;

  /**
   * Sets up a {@link CloudSdk} to be used for App Engine deploy.
   */
  public void setUpDeployCloudSdk(Path credentialFile, MessageConsoleStream normalOutputStream) {
    Preconditions.checkNotNull(credentialFile, "credential required for deploying");
    Preconditions.checkArgument(Files.exists(credentialFile), "non-existing credential file");
    Preconditions.checkState(cloudSdk == null, "CloudSdk already set up");

    // Structured deploy result (in JSON format) goes to stdout, so prepare to capture that.
    stdOutCaptor = new StringBuilderProcessOutputLineListener();

    // Normal operation output goes to stderr.
    MessageConsoleStream stdErrOutputStream = normalOutputStream;
    CloudSdk.Builder cloudSdkBuilder = getBaseCloudSdkBuilder(stdErrOutputStream);
    cloudSdkBuilder.appCommandCredentialFile(credentialFile.toFile());
    cloudSdkBuilder.addStdOutLineListener(stdOutCaptor);
    cloudSdk = cloudSdkBuilder.build();
  }

  /**
   * Sets up a {@link CloudSdk} to be used for App Engine standard staging.
   *
   * @param javaHome JDK/JRE to 1) run {@code com.google.appengine.tools.admin.AppCfg} from
   *     {@code appengine-tools-api.jar}; and 2) compile JSPs during staging
   */
  public void setUpStandardStagingCloudSdk(Path javaHome,
      MessageConsoleStream stdoutOutputStream, MessageConsoleStream stderrOutputStream) {
    Preconditions.checkState(cloudSdk == null, "CloudSdk already set up");

    CloudSdk.Builder cloudSdkBuilder = getBaseCloudSdkBuilder(stderrOutputStream);
    if (javaHome != null) {
      cloudSdkBuilder.javaHome(javaHome);
    }
    cloudSdkBuilder.addStdOutLineListener(
        new MessageConsoleWriterOutputLineListener(stdoutOutputStream));
    cloudSdk = cloudSdkBuilder.build();
  }

  private CloudSdk.Builder getBaseCloudSdkBuilder(MessageConsoleStream stdErrListener) {
    CollectingLineListener errorMessageCollector = new CollectingLineListener(IS_ERROR_LINE);

    return new CloudSdk.Builder()
        .addStdErrLineListener(new MessageConsoleWriterOutputLineListener(stdErrListener))
        .addStdErrLineListener(errorMessageCollector)
        .startListener(new StoreProcessObjectListener())
        .exitListener(new ProcessExitRecorder(errorMessageCollector))
        .appCommandMetricsEnvironment(CloudToolsInfo.METRICS_NAME)
        .appCommandMetricsEnvironmentVersion(CloudToolsInfo.getToolsVersion())
        .appCommandOutputFormat("json");
  }

  public CloudSdk getCloudSdk() {
    Preconditions.checkNotNull(cloudSdk, "wrapper not set up");
    return cloudSdk;
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

  private class StoreProcessObjectListener implements ProcessStartListener {
    @Override
    public void onStart(Process process) {
      synchronized (CloudSdkProcessWrapper.this) {
        CloudSdkProcessWrapper.this.process = process;
        if (interrupted) {
          CloudSdkProcessWrapper.this.process.destroy();
        }
      }
    }
  }

  @VisibleForTesting
  class ProcessExitRecorder implements ProcessExitListener {

    private final CollectingLineListener errorMessageCollector;

    @VisibleForTesting
    ProcessExitRecorder(CollectingLineListener errorMessageCollector) {
      this.errorMessageCollector = errorMessageCollector;
    }

    @Override
    public void onExit(int exitCode) {
      if (exitCode != 0) {
        exitStatus = StatusUtil.error(this, getErrorMessage(exitCode), exitCode);
      } else {
        exitStatus = Status.OK_STATUS;
      }
    }

    private String getErrorMessage(int exitCode) {
      if (errorMessageCollector != null) {
        List<String> lines = errorMessageCollector.getCollectedMessages();
        if (!lines.isEmpty()) {
          return Joiner.on('\n').join(lines);
        }
      }
      return Messages.getString("cloudsdk.process.failed", exitCode);
    }
  }
}
