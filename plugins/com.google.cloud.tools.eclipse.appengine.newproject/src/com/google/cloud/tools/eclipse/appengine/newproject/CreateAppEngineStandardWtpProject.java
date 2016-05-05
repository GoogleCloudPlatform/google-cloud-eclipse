package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

/**
* Utility to make a new Eclipse project with the App Engine Standard facets in the workspace.  
*/
class CreateAppEngineStandardWtpProject extends WorkspaceModifyOperation {
  
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
  public void execute(IProgressMonitor monitor) throws InvocationTargetException, CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 100);
    
    URI location = config.getEclipseProjectLocationUri();
    
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject newProject = config.getProject();
    
    String name = newProject.getName();
    final IProjectDescription description = workspace.newProjectDescription(name);
    description.setLocationURI(location);
    
    CreateProjectOperation operation = new CreateProjectOperation(
        description, "Creating new App Engine Project");
    try {
      operation.execute(progress.newChild(20), uiInfoAdapter);
      IFacetedProject facetedProject = ProjectFacetsManager.create(
          newProject, true, progress.newChild(40));
      IProjectFacet javaFacet = ProjectFacetsManager.getProjectFacet("jst.java");
      // todo setup sourcepath
      facetedProject.installProjectFacet(javaFacet.getVersion("1.7"), null, monitor);

      CodeTemplates.materialize(newProject, config, progress.newChild(40));
      
    } catch (ExecutionException ex) {
      throw new InvocationTargetException(ex);
    } finally {
      progress.done();
    }
  }

}
