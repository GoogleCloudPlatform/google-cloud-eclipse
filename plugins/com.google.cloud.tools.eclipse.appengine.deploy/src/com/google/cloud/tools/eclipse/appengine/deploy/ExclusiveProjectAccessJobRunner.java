package com.google.cloud.tools.eclipse.appengine.deploy;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Runs a {@link AppEngineDeployJob} in an {@link IWorkspace} using a lock to prevent any changes to the project while
 * the operation is in progress.
 *
 */
// Main purpose of this class is to separate Eclipse' job execution from other logic to enhance testability
// TODO move to common bundle to reuse across plugin
public class ExclusiveProjectAccessJobRunner {

  public void runJob(IWorkspaceRunnable job, IProject project, IProgressMonitor monitor) throws CoreException {
    IWorkspace workspace = project.getWorkspace();
    workspace.run(
        job,
        workspace.getRuleFactory().createRule(project),
        0 /* flags */,
        monitor);
  }

}
