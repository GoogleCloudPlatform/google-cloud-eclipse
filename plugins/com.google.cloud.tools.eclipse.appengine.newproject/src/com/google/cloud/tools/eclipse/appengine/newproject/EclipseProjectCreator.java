package com.google.cloud.tools.eclipse.appengine.newproject;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

/**
* Utility to make a new Eclipse project with the App Engine Standard facets in the workspace.  
*/
class EclipseProjectCreator {
  
  /**
   * @return status of project creation
   */
  static IStatus makeNewProject(
      AppEngineStandardProjectConfig config, IProgressMonitor monitor,
      final Shell shell, final IWizardContainer container) {
    
    URI location = config.getEclipseProjectLocationUri();
    
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject newProject = config.getProject();
    String name = newProject.getName();
    final IProjectDescription description = workspace.newProjectDescription(name);
    description.setLocationURI(location);
    
    IRunnableWithProgress runnable = new CreateNewProjectOperation(description, shell);    
    try {
      boolean fork = true;
      boolean cancelable = true;
      container.run(fork, cancelable, runnable);
      return Status.OK_STATUS;
    } catch (InterruptedException ex) {
      return Status.CANCEL_STATUS;
    } catch (InvocationTargetException ex) {
      return new Status(Status.ERROR, ex.getMessage(), 1, "", null);
    }
  }
  
  private static final class CreateNewProjectOperation implements IRunnableWithProgress {
    private final IProjectDescription description;
    private final Shell shell;

    private CreateNewProjectOperation(IProjectDescription description, Shell shell) {
      this.description = description;
      this.shell = shell;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
      CreateProjectOperation operation = new CreateProjectOperation(
          description, "Creating new App Engine Project");
      try {
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=219901
        operation.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(shell));
      } catch (ExecutionException ex) {
        throw new InvocationTargetException(ex);
      }
    }
  }

}
