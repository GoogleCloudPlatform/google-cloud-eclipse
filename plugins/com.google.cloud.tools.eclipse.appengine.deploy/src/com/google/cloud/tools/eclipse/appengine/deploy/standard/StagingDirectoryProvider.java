package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.resources.IProject;

/**
 * Common interface class to obtain the staging directory for App Engine Standard deploy.
 */
public interface StagingDirectoryProvider {

  /**
   * @return path to the directory to be used for staging
   * @see com.google.cloud.tools.eclipse.appengine.deploy.standard.ProjectToStagingExporter#writeProjectToStageDir(IProject, String)
   * @see com.google.cloud.tools.eclipse.appengine.deploy.standard.StandardDeployCommandHandler
   */
  String get();

}
