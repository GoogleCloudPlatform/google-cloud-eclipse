package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.internal.ServerPlugin;

import com.google.cloud.tools.eclipse.appengine.deploy.FacetedProjectHelper;
import com.google.common.annotations.VisibleForTesting;

/**
 * Command handler to deploy an App Engine web application project to App Engine Standard.
 * <p>
 * It copies the project's exploded WAR to a staging directory and then executes staging and deploy operations
 * provided by the App Engine Plugins Core Library.
 */
public class StandardDeployCommandHandler extends AbstractHandler {

  private static final IPath defaultStagingDir = ServerPlugin.getInstance().getStateLocation();

  private ProjectFromSelectionHelper helper;
  
  public StandardDeployCommandHandler() {
    this(new FacetedProjectHelper());
  }
  
  @VisibleForTesting
  StandardDeployCommandHandler(FacetedProjectHelper facetedProjectHelper) {
      this.helper = new ProjectFromSelectionHelper(facetedProjectHelper);
  }
  
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    try {
      IProject project = helper.getProject(event);
      if (project != null) {
        final IProject project1 = project;
        StandardDeployJob deploy = new StandardDeployJob(new ProjectToStagingExporter(), defaultStagingDir, project1);
        deploy.schedule();
      }
      // return value must be null, reserved for future use
      return null;
    } catch (CoreException coreException) {
      throw new ExecutionException("Failed to export the project as exploded WAR", coreException);
    }
  }

}
