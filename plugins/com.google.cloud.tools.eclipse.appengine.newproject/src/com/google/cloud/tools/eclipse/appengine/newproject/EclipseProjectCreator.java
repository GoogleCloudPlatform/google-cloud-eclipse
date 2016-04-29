package com.google.cloud.tools.eclipse.appengine.newproject;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.ide.undo.CreateProjectOperation;

/**
* Utility to make a new Eclipse project with the App Engine Standard facets in the workspace.  
*/
class CreateAppEngineStandardWtpProject implements IRunnableWithProgress {
  
  private final AppEngineStandardProjectConfig config;
  private final IAdaptable uiInfoAdapter;

  CreateAppEngineStandardWtpProject(AppEngineStandardProjectConfig config, IAdaptable uiInfoAdapter) {
    if (config == null) {
      throw new NullPointerException("Null App Engine configuration");
    }
    this.config = config;
    this.uiInfoAdapter = uiInfoAdapter;
  }

  @Override
  public void run(IProgressMonitor monitor) throws InvocationTargetException {
    
    URI location = config.getEclipseProjectLocationUri();
    
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject newProject = config.getProject();
    
    String name = newProject.getName();
    final IProjectDescription description = workspace.newProjectDescription(name);
    description.setLocationURI(location);
    
    CreateProjectOperation operation = new CreateProjectOperation(
        description, "Creating new App Engine Project");
    try {
      operation.execute(monitor, uiInfoAdapter);
      createCode(monitor, newProject);
    } catch (ExecutionException | CoreException ex) {
      throw new InvocationTargetException(ex);
    }
  }

  // todo design a template system for this.
  /**
   * Set up the sample code.
   */
  private void createCode(IProgressMonitor monitor, IProject newProject) throws CoreException {
    boolean force = true;
    boolean local = true;
    IFolder src = newProject.getFolder("src");
    if (!src.exists()) {
      src.create(force, local, monitor);
    }
    IFolder main = createChildFolder("main", src, monitor);
    IFolder java = createChildFolder("java", main, monitor);
    IFolder webapp = createChildFolder("webapp", main, monitor);
    IFolder webinf = createChildFolder("WEB-INF", webapp, monitor);
    IFolder test = createChildFolder("test", src, monitor);
  }

  private IFolder createChildFolder(String name, IFolder parent, IProgressMonitor monitor) 
      throws CoreException {
    boolean force = true;
    boolean local = true;
    IFolder child = parent.getFolder(name);
    if (!child.exists()) {
      child.create(force, local, monitor);
    }
    return child;
  }
}
