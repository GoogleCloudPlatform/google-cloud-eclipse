package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.handlers.HandlerUtil;

import com.google.cloud.tools.eclipse.appengine.deploy.ExclusiveProjectAccessJobRunner;
import com.google.cloud.tools.eclipse.appengine.deploy.FacetedProjectHelper;
import com.google.common.annotations.VisibleForTesting;

/**
 * Command handler to deploy an App Engine web application project to App Engine Standard.
 * <p>
 * It copies the project's exploded WAR to a staging directory and then executes staging and deploy operations
 * provided by the App Engine Plugins Core Library.
 */
public class StandardDeployCommandHandler extends AbstractHandler {

  private ExclusiveProjectAccessJobRunner jobRunner;
  private ProjectFromSelectionHelper helper;
  
  public StandardDeployCommandHandler() {
    this(new FacetedProjectHelper(), new ExclusiveProjectAccessJobRunner());
  }
  
  @VisibleForTesting
  StandardDeployCommandHandler(FacetedProjectHelper facetedProjectHelper,
                               ExclusiveProjectAccessJobRunner jobRunner) {
      this.helper = new ProjectFromSelectionHelper(facetedProjectHelper);
      this.jobRunner = jobRunner;
  }
  
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    try {
      IProject project = getAppEngineStandardProjectFromSelection(event);
      if (project != null) {
        deployProject(event, project);
      }
      // return value must be null, reserved for future use
      return null;
    } catch (CoreException coreException) {
      throw new ExecutionException("Failed to export the project as exploded WAR", coreException);
    }
  }

  private IProject getAppEngineStandardProjectFromSelection(ExecutionEvent event) throws CoreException,
                                                                                         ExecutionException {
    return helper.getProject(event);
  }

  private void deployProject(ExecutionEvent event, final IProject project) throws CoreException, ExecutionException {
    jobRunner.runJob(
        new StandardDeployJob(new ProjectToStagingExporter(),
                              new DialogStagingDirectoryProvider(HandlerUtil.getActiveShell(event)),
                              project),
        project,
        new NullProgressMonitor());
  }

  @Override
  public boolean isEnabled() {
    // TODO implement properly
    return true;
  }
}
