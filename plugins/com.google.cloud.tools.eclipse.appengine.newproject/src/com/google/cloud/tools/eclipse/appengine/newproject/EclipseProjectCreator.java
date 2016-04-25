package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

/**
* Utility to make a new Eclipse project with the App Engine Standard Nature in the workspace.  
*/
class EclipseProjectCreator {

  /**
   * @return true on successful project creation; false otherwise
   */
  static boolean makeNewProject(AppEngineStandardProjectConfig config) {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = workspaceRoot.getProject(config.getEclipseProjectName());
    return false;
  }

}
