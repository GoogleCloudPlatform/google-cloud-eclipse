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
    super(Messages.getString("deploy.standard.runnable.name"));
    setRule(project);
    projectToStagingExporter = exporter;
    this.stagingDirectoryProvider = stagingDirectoryProvider;
    this.project = project;
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    String stageDir = stagingDirectoryProvider.get();
    if (stageDir == null) {
      // TODO move /com.google.cloud.tools.eclipse.appengine.localserver/src/com/google/cloud/tools/eclipse/appengine/localserver/Activator.java
      // to a bundle that can be a common dependency for all bundles, and use PLUGIN_ID instead of string literal
      throw new CoreException(new Status(IStatus.ERROR, "pluginid", "Staging directory cannot be null"));
    }
    writeProjectToStageDir(project, stageDir);
    verifyProjectInStageDir(stageDir);
    //TODO run stage and deploy operations
    return Status.OK_STATUS;
  }

  void writeProjectToStageDir(IProject project, String stageDir) throws CoreException {
    projectToStagingExporter.writeProjectToStageDir(project, stageDir);
  }

  void verifyProjectInStageDir(String stageDir) {
    // TODO verify if the export was successful by checking if appengine-web.xml exists
  }

}