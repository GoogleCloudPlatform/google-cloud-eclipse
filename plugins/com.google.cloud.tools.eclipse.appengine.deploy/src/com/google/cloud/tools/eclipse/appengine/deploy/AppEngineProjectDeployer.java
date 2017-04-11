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

import com.google.cloud.tools.appengine.api.deploy.DefaultDeployConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineDeployment;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Deploys a staged App Engine project. The project must be staged first (e.g. using {@link
 * DeployStaging}). This class will take the staged project and deploy it to App Engine using
 * {@link CloudSdk}.
 */
public class AppEngineProjectDeployer {

  @VisibleForTesting
  static final List<String> APP_ENGINE_CONFIG_FILES = Collections.unmodifiableList(Arrays.asList(
      "cron.yaml", "dispatch.yaml", "dos.yaml", "index.yaml", "queue.yaml"));

  /**
   * @param optionalConfigurationFilesDirectory if not {@code null}, searches optional configuration
   * files (such as {@code cron.yaml}) in this directory and deploys them together
   */
  public void deploy(IPath stagingDirectory, CloudSdk cloudSdk,
                     DefaultDeployConfiguration configuration,
                     IPath optionalConfigurationFilesDirectory, IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    SubMonitor progress = SubMonitor.convert(monitor, 1);
    progress.setTaskName(Messages.getString("task.name.deploy.project")); //$NON-NLS-1$
    try {
      List<File> deployables =
          computeDeployables(stagingDirectory, optionalConfigurationFilesDirectory);
      configuration.setDeployables(deployables);
      CloudSdkAppEngineDeployment deployment = new CloudSdkAppEngineDeployment(cloudSdk);
      deployment.deploy(configuration);
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
}
