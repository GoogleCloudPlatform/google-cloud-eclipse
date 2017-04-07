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

import com.google.cloud.tools.eclipse.appengine.deploy.Messages;
import com.google.common.base.Preconditions;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jst.j2ee.internal.deployables.J2EEFlexProjDeployable;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.server.core.util.PublishHelper;

/**
 * Writes a WAR file of a project, or the exploded contents of it to a destination directory.
 */
public class WarPublisher {

  /**
   * It does a smart export, i.e. considers the resources to be copied and if the destination
   * directory already contains resources those will be deleted if they are not part of the exploded
   * WAR.
   */
  public static void publishExploded(IProject project, IPath destination, IProgressMonitor monitor)
      throws CoreException {
    publish(project, destination, true /* exploded */, monitor);
  }

  public static void publishWar(IProject project, IPath destination, IProgressMonitor monitor)
      throws CoreException {
    publish(project, destination, false /* exploded */, monitor);
  }

  private static void publish(IProject project, IPath destination, boolean exploded,
      IProgressMonitor monitor) throws CoreException {
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }
    Preconditions.checkNotNull(project, "project is null"); //$NON-NLS-1$
    Preconditions.checkNotNull(destination, "destination is null"); //$NON-NLS-1$
    Preconditions.checkArgument(!destination.isEmpty(), "destination is empty path"); //$NON-NLS-1$

    SubMonitor progress = SubMonitor.convert(monitor, 100);
    progress.setTaskName(Messages.getString("task.name.publish.war"));

    PublishHelper publishHelper = new PublishHelper(null);
    J2EEFlexProjDeployable deployable =
        new J2EEFlexProjDeployable(project, ComponentCore.createComponent(project));

    if (exploded) {
      publishHelper.publishSmart(deployable.members(), destination, progress.newChild(100));
    } else {
      publishHelper.publishZip(deployable.members(), destination, progress.newChild(100));
    }
  }
}
