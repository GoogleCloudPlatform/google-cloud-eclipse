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

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.appengine.operations.Deployment;
import com.google.cloud.tools.appengine.configuration.DeployConfiguration;
import com.google.cloud.tools.appengine.operations.CloudSdk;
import com.google.cloud.tools.eclipse.appengine.deploy.util.CloudSdkProcessWrapper;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * Deploys a staged App Engine project. The project must be staged first (e.g. using {@link
 * CloudSdkStagingHelper}). This class will take the staged project and deploy it to App Engine
 * using {@link CloudSdk}.
 */
public class AppEngineProjectDeployer {

  @VisibleForTesting
  static final List<String> APP_ENGINE_CONFIG_FILES = Collections.unmodifiableList(Arrays.asList(
      "cron.yaml", "dispatch.yaml", "dos.yaml", "index.yaml", "queue.yaml"));

  private final CloudSdkProcessWrapper cloudSdkProcessWrapper = new CloudSdkProcessWrapper();

  /**
   * @param optionalConfigurationFilesDirectory if not {@code null}, searches optional configuration
   *     files (such as {@code cron.yaml}) in this directory and deploys them together
   */
  public IStatus deploy(IPath stagingDirectory,
      DeployPreferences deployPreferences, IPath optionalConfigurationFilesDirectory,
      MessageConsoleStream stdoutOutputStream, IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    SubMonitor progress = SubMonitor.convert(monitor, 1);
    progress.setTaskName(Messages.getString("task.name.deploy.project")); //$NON-NLS-1$
    try {
      List<File> files =
          computeDeployables(stagingDirectory, optionalConfigurationFilesDirectory);
      List<Path> deployables = new ArrayList<>();
      for (File file : files) {
        deployables.add(file.toPath());
      }
      
      DeployConfiguration configuration =
          DeployPreferencesConverter.toDeployConfiguration(deployPreferences, deployables);
      try { 
        Deployment deployment =
            cloudSdkProcessWrapper.getAppEngineDeployment(stdoutOutputStream);
        deployment.deploy(configuration);
      } catch (AppEngineException ex) {
        return StatusUtil.error(this, "Error deploying project: " + ex.getMessage(), ex);
      }
      return cloudSdkProcessWrapper.getExitStatus();
    } finally {
      progress.worked(1);
    }
  }

  @VisibleForTesting
  static List<File> computeDeployables(
      IPath stagingDirectory, IPath optionalConfigurationFilesDirectory) {
    List<File> deployables = new ArrayList<>();
    deployables.add(stagingDirectory.append("app.yaml").toFile()); //$NON-NLS-1$

    if (optionalConfigurationFilesDirectory != null) {
      for (String configFile : APP_ENGINE_CONFIG_FILES) {
        File file = optionalConfigurationFilesDirectory.append(configFile).toFile();
        if (file.exists()) {
          deployables.add(file);
        }
      }
    }
    return deployables;
  }

  public void interrupt() {
    cloudSdkProcessWrapper.interrupt();
  }

  public String getJsonDeployResult() {
    Preconditions.checkNotNull(cloudSdkProcessWrapper);
    return cloudSdkProcessWrapper.getStdOutAsString();
  }
}
