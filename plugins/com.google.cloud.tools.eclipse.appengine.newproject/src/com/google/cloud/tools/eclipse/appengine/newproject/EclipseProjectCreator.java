package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
* Utility to make a new Eclipse project with the App Engine Standard facets in the workspace.  
*/
class EclipseProjectCreator {

  /**
   * @return status of project creation
   */
  static IStatus makeNewProject(AppEngineStandardProjectConfig config, IProgressMonitor monitor) {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = workspaceRoot.getProject(config.getEclipseProjectName());
    return Status.OK_STATUS;
  }

}
