package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.google.cloud.tools.eclipse.appengine.deploy.Messages;
import com.google.common.base.Preconditions;

public class StandardDeployJob extends WorkspaceJob {

  private final ProjectToStagingExporter projectToStagingExporter;
  private final IProject project;
  private final IPath stagingDir;

  public StandardDeployJob(ProjectToStagingExporter exporter,
                           IPath stagingDir,
                           IProject project) {
    super(Messages.getString("deploy.standard.runnable.name")); //$NON-NLS-1$

    Preconditions.checkNotNull(exporter, "exporter is null");
    Preconditions.checkNotNull(stagingDir, "stagingDir is null");
    Preconditions.checkNotNull(project, "project is null");

    setRule(project);
    this.stagingDir = stagingDir;
    projectToStagingExporter = exporter;
    this.project = project;
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    if (stagingDir.toFile().exists()) {
      projectToStagingExporter.writeProjectToStageDir(project, stagingDir);
      //TODO run stage and deploy operations
      return Status.OK_STATUS;
    } else {
      return new Status(IStatus.ERROR,
                        "com.google.cloud.tools.eclipse.appengine.localserver",
                        Messages.getString("deploy.job.stagingdir.missing")); //$NON-NLS-1$
    }
  }
}
