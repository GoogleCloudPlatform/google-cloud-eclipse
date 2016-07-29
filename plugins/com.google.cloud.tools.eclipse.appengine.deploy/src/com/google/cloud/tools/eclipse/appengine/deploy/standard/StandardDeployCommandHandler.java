package com.google.cloud.tools.eclipse.appengine.deploy.standard;

import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.ui.handlers.HandlerUtil;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.deploy.AppEngineProjectDeployer;
import com.google.cloud.tools.eclipse.appengine.deploy.CleanupOldDeploysJob;
import com.google.cloud.tools.eclipse.appengine.deploy.Messages;
import com.google.cloud.tools.eclipse.appengine.login.GoogleLoginService;
import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;
import com.google.cloud.tools.eclipse.util.ProjectFromSelectionHelper;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
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
        launchDeployJob(project, new SameShellProvider(HandlerUtil.getActiveShell(event)));
      }
      // return value must be null, reserved for future use
      return null;
    } catch (CoreException | IOException exception) {
      throw new ExecutionException(Messages.getString("deploy.failed.error.message"), exception); //$NON-NLS-1$
    }
  }

  private void launchDeployJob(IProject project, IShellProvider shellProvider) throws IOException, CoreException {
    IPath workDirectory = createWorkDirectory();
    Credential credential = login(shellProvider);
    
    StandardDeployJob deploy =
        new StandardDeployJob(new ExplodedWarPublisher(),
                              new StandardProjectStaging(),
                              new AppEngineProjectDeployer(),
                              workDirectory,
                              project,
                              credential);
    deploy.addJobChangeListener(new JobChangeAdapter() {

      @Override
      public void done(IJobChangeEvent event) {
        super.done(event);
        launchCleanupJob();
      }
    });
    deploy.schedule();
  }

  private IPath createWorkDirectory() throws IOException {
    String now = Long.toString(System.currentTimeMillis());
    IPath workDirectory = getTempDir().append(now);
    Files.createDirectories(workDirectory.toFile().toPath());
    return workDirectory;
  }

  private Credential login(IShellProvider shellProvider) throws IOException, CoreException {
    Credential credential = new GoogleLoginService().getActiveCredential(shellProvider);
    if (credential == null) {
      throw new CoreException(StatusUtil.error(getClass(), Messages.getString("login.failed")));
    }
    return credential;
  }

  private void launchCleanupJob() {
    new CleanupOldDeploysJob(getTempDir()).schedule();
  }

  private IPath getTempDir() {
    return Platform.getStateLocation(Platform.getBundle("com.google.cloud.tools.eclipse.appengine.deploy"))
        .append("tmp");
  }
}
