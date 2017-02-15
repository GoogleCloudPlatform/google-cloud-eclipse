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
import java.util.Collections;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Deploys a staged App Engine project. The project must be staged first (e.g. in case of App Engine
 * Standard project using
 * {@link com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardProjectStaging
 * StandardProjectStaging}) This class will take the staged project and deploy it to App Engine
 * using {@link CloudSdk}.
 *
 */
public class AppEngineProjectDeployer {

  public void deploy(IPath stagingDirectory, CloudSdk cloudSdk,
                     DefaultDeployConfiguration configuration,
                     IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }

    SubMonitor progress = SubMonitor.convert(monitor, 1);
    progress.setTaskName(Messages.getString("task.name.deploy.project")); //$NON-NLS-1$
    try  {
      configuration.setDeployables(Collections.singletonList(stagingDirectory.append("app.yaml").toFile())); //$NON-NLS-1$
      CloudSdkAppEngineDeployment deployment = new CloudSdkAppEngineDeployment(cloudSdk);
      deployment.deploy(configuration);
    } finally {
      progress.worked(1);
    }
  }
}
