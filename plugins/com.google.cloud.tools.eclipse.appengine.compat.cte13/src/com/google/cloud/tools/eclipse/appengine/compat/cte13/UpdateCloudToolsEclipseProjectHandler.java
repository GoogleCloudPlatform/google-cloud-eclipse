
package com.google.cloud.tools.eclipse.appengine.compat.cte13;

import com.google.cloud.tools.eclipse.ui.util.ProjectFromSelectionHelper;
import java.text.MessageFormat;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

public class UpdateCloudToolsEclipseProjectHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    final IProject project = ProjectFromSelectionHelper.getFirstProject(event);
    if (!CloudToolsEclipseProjectUpdater.hasOldContainers(project)) {
      throw new ExecutionException("Project appears to be up-to-date");
    }
    Job updateJob = new WorkspaceJob(MessageFormat.format("Updating {0}", project.getName())) {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        return CloudToolsEclipseProjectUpdater.updateProject(project, SubMonitor.convert(monitor));
      }
    };
    updateJob.setRule(project.getWorkspace().getRoot());
    updateJob.setUser(true);
    updateJob.schedule();
    return null;
  }

}
