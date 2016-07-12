package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.google.cloud.tools.eclipse.appengine.deploy.Messages;

public class StandardDeployJob extends WorkspaceJob {
  
  private ProjectToStagingExporter projectToStagingExporter;
  private StagingDirectoryProvider stagingDirectoryProvider;
  private IProject project;

  public StandardDeployJob(ProjectToStagingExporter exporter,
                           StagingDirectoryProvider stagingDirectoryProvider,
                           IProject project) {
    super(Messages.getString("deploy.standard.runnable.name")); //$NON-NLS-1$
    setRule(project);
    projectToStagingExporter = exporter;
    this.stagingDirectoryProvider = stagingDirectoryProvider;
    this.project = project;
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    String stageDir = stagingDirectoryProvider.get();
    if (stageDir != null) {
      writeProjectToStageDir(project, stageDir);
      verifyProjectInStageDir(stageDir);
      //TODO run stage and deploy operations
      return Status.OK_STATUS;
    } else {
      return Status.CANCEL_STATUS;
    }
  }

  void writeProjectToStageDir(IProject project, String stageDir) throws CoreException {
    projectToStagingExporter.writeProjectToStageDir(project, stageDir);
  }

  void verifyProjectInStageDir(String stageDir) {
    // TODO verify if the export was successful by checking if appengine-web.xml exists
  }

}