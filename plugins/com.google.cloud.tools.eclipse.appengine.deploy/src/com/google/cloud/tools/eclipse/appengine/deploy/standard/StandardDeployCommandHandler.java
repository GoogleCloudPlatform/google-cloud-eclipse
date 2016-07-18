package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

import com.google.cloud.tools.eclipse.appengine.deploy.AppEngineProjectDeployer;
import com.google.cloud.tools.eclipse.appengine.deploy.Messages;
import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;
import com.google.cloud.tools.eclipse.util.ProjectFromSelectionHelper;
import com.google.common.annotations.VisibleForTesting;

/**
 * Command handler to deploy an App Engine web application project to App Engine Standard.
 * <p>
 * It copies the project's exploded WAR to a staging directory and then executes staging and deploy operations
 * provided by the App Engine Plugins Core Library.
 */
public class StandardDeployCommandHandler extends AbstractHandler {

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
        String now = Long.toString(System.currentTimeMillis());
        StandardDeployJob deploy =
            new StandardDeployJob(new ExplodedWarPublisher(),
                                  new StandardProjectStaging(),
                                  new AppEngineProjectDeployer(),
                                  getTempDir().append(now),
                                  project);
        deploy.schedule();
      }
      // return value must be null, reserved for future use
      return null;
    } catch (CoreException coreException) {
      throw new ExecutionException(Messages.getString("deploy.failed.error.message"), coreException); //$NON-NLS-1$
    }
  }

  private IPath getTempDir() {
    return Platform.getStateLocation(Platform.getBundle("com.google.cloud.tools.eclipse.appengine.deploy")).append("tmp"); //$NON-NLS-1$ //$NON-NLS-2$
  }
}
