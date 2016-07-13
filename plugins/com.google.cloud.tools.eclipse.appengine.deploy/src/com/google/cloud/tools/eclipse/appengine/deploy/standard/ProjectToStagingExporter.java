package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.j2ee.internal.deployables.J2EEFlexProjDeployable;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.server.core.util.PublishHelper;

import com.google.common.base.Preconditions;

/**
 * Writes the exploded WAR file of a project to a staging directory.  
 */
public class ProjectToStagingExporter {

  /**
   * It does a smart export, i.e. considers the resources to be copied and
   * if the destination directory already contains resources those will be deleted if they are not part of the
   * exploded WAR.
   */
  public void writeProjectToStageDir(IProject project, IPath stageDir) throws CoreException {
    Preconditions.checkNotNull(project, "project is null");
    Preconditions.checkNotNull(stageDir, "stagedir is null");
    Preconditions.checkArgument(!stageDir.isEmpty(), "stagedir is empty path");
    
    PublishHelper publishHelper = new PublishHelper(null);
    J2EEFlexProjDeployable deployable = new J2EEFlexProjDeployable(project, ComponentCore.createComponent(project));
    publishHelper.publishSmart(deployable.members(), stageDir, new NullProgressMonitor());
  }
}
