package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jst.j2ee.internal.deployables.J2EEFlexProjDeployable;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.server.core.util.PublishHelper;

import com.google.cloud.tools.eclipse.appengine.deploy.Messages;
import com.google.common.base.Preconditions;

/**
 * Writes the exploded WAR file of a project to a staging directory.
 */
public class ExplodedWarPublisher {

  /**
   * It does a smart export, i.e. considers the resources to be copied and
   * if the destination directory already contains resources those will be deleted if they are not part of the
   * exploded WAR.
   */
  public void publish(IProject project, IPath destination, IProgressMonitor monitor) throws CoreException {
    Preconditions.checkNotNull(project, "project is null"); //$NON-NLS-1$
    Preconditions.checkNotNull(destination, "destination is null"); //$NON-NLS-1$
    Preconditions.checkArgument(!destination.isEmpty(), "destination is empty path"); //$NON-NLS-1$

    SubMonitor progress = SubMonitor.convert(monitor, 100);
    progress.setTaskName(Messages.getString("task.name.publish.war"));

    PublishHelper publishHelper = new PublishHelper(null);
    J2EEFlexProjDeployable deployable = new J2EEFlexProjDeployable(project, ComponentCore.createComponent(project));
    publishHelper.publishSmart(deployable.members(), destination, progress.newChild(100));
  }
}
