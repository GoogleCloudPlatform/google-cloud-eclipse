package com.google.cloud.tools.eclipse.appengine.newproject;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.ide.undo.CreateProjectOperation;

/**
* Utility to make a new Eclipse project with the App Engine Standard facets in the workspace.  
*/
class EclipseProjectCreator {
  
  /**
   * @return an operation that creates a new project when run
   */
  static IRunnableWithProgress makeNewProject(
      AppEngineStandardProjectConfig config, IProgressMonitor monitor, final IAdaptable uiInfoAdapter) {
    
    URI location = config.getEclipseProjectLocationUri();
    
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject newProject = config.getProject();
    String name = newProject.getName();
    final IProjectDescription description = workspace.newProjectDescription(name);
    description.setLocationURI(location);
    
    return new CreateNewProjectOperation(description, uiInfoAdapter); 
  }
  
  private static final class CreateNewProjectOperation implements IRunnableWithProgress {
    private final IProjectDescription description;
    private final IAdaptable uiInfoAdapter;

    private CreateNewProjectOperation(IProjectDescription description, IAdaptable uiInfoAdapter) {
      this.description = description;
      this.uiInfoAdapter = uiInfoAdapter;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
      CreateProjectOperation operation = new CreateProjectOperation(
          description, "Creating new App Engine Project");
      try {
        operation.execute(monitor, uiInfoAdapter);
      } catch (ExecutionException ex) {
        throw new InvocationTargetException(ex);
      }
    }
  }

}
