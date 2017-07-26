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

package com.google.cloud.tools.eclipse.sdk;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.appengine.cloudsdk.process.StringBuilderProcessOutputLineListener;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class CloudSdkProcessFacade {

  private static final String ERROR_MESSAGE_PREFIX = "ERROR:";
  private static final Predicate<String> IS_ERROR_LINE = new Predicate<String>() {
    @Override
    public boolean apply(String line) {
      return line != null && line.startsWith(ERROR_MESSAGE_PREFIX);
    }
  };

  private CloudSdk cloudSdk;

  public Process process;
  public boolean canceled;
  private IStatus exitStatus = Status.OK_STATUS;
  private ProcessOutputLineListener stdOutCaptor;

  private CloudSdkProcessFacade() {  // empty private constructor
  }

  /**
   * Creates a wrapper/facade of {@link CloudSdk} to be used for App Engine deploy.
   */
  public static CloudSdkProcessFacade forDeploy(Path credentialFile,
      ProcessOutputLineListener normalOutputListener) {
    Preconditions.checkNotNull(credentialFile);

    // Normal operation output goes to stderr.
    ProcessOutputLineListener stdErrListener = normalOutputListener;
    // Structured deploy result (in JSON format) goes to stdout, so prepare to capture that.
    ProcessOutputLineListener stdOutCaptor = new StringBuilderProcessOutputLineListener();

    CloudSdkProcessFacade facade = new CloudSdkProcessFacade();
    CloudSdk.Builder cloudSdkBuilder = facade.getBaseCloudSdkBuilder(stdErrListener);
    cloudSdkBuilder.appCommandCredentialFile(credentialFile.toFile());
    cloudSdkBuilder.addStdOutLineListener(stdOutCaptor);
    facade.cloudSdk = cloudSdkBuilder.build();
    facade.stdOutCaptor = stdOutCaptor;

    return facade;
  }

  /**
   * Creates a wrapper/facade of {@link CloudSdk} to be used for App Engine standard staging.
   *
   * @param javaHome JDK/JRE to run gcloud, which is also used to compile JSPs during staging
   */
  public static CloudSdkProcessFacade forStandardStaging(Path javaHome,
      ProcessOutputLineListener stdOutListener, ProcessOutputLineListener stdErrListener) {
    CloudSdkProcessFacade cloudSdkRunner = new CloudSdkProcessFacade();
    CloudSdk.Builder cloudSdkBuilder =
        cloudSdkRunner.getBaseCloudSdkBuilder(stdErrListener);
    if (javaHome != null) {
      cloudSdkBuilder.javaHome(javaHome);
    }
    cloudSdkBuilder.addStdOutLineListener(stdOutListener);
    cloudSdkRunner.cloudSdk = cloudSdkBuilder.build();

    return cloudSdkRunner;
  }

  private CloudSdk.Builder getBaseCloudSdkBuilder(ProcessOutputLineListener stdErrListener) {
    CollectingLineListener errorMessageCollector = new CollectingLineListener(IS_ERROR_LINE);

    return new CloudSdk.Builder()
        .addStdErrLineListener(stdErrListener)
        .addStdErrLineListener(errorMessageCollector)
        .startListener(new StoreProcessObjectListener())
        .exitListener(new ProcessExitRecorder(errorMessageCollector))
        .appCommandMetricsEnvironment(CloudToolsInfo.METRICS_NAME)
        .appCommandMetricsEnvironmentVersion(CloudToolsInfo.getToolsVersion())
        .appCommandOutputFormat("json");
  }

  public CloudSdk getCloudSdk() {
    return cloudSdk;
  }

  public void cancel() {
    canceled = true;  // not to miss destruction due to race condition
    if (process != null) {
      process.destroy();
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
    public void onStart(Process proces) {
      process = proces;
      if (canceled) {
        process.destroy();
      }
    }
  }

  private class ProcessExitRecorder implements ProcessExitListener {

    private final CollectingLineListener errorMessageCollector;

    private ProcessExitRecorder(CollectingLineListener errorMessageCollector) {
      this.errorMessageCollector = errorMessageCollector;
    }

    @Override
    public void onExit(int exitCode) {
      if (exitCode != 0) {
        exitStatus = StatusUtil.error(this, getErrorMessage(exitCode));
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
