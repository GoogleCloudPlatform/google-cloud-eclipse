package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
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
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    final IProject newProject = config.getProject();
    URI location = config.getEclipseProjectLocationUri();

    String name = newProject.getName();
    final IProjectDescription description = workspace.newProjectDescription(name);
    description.setLocationURI(location);
    
    CreateProjectOperation operation = new CreateProjectOperation(
        description, "Creating new App Engine Project");
    try {
      operation.execute(monitor, uiInfoAdapter);
      CodeTemplates.materialize(newProject, config, monitor);
    } catch (ExecutionException ex) {
      throw new InvocationTargetException(ex, ex.getMessage());
    }

    IFacetedProject facetedProject = ProjectFacetsManager.create(
        newProject, true, monitor);
    AppEngineStandardFacet.installAppEngineFacet(
        facetedProject, true /* installDependentFacets */, monitor);
    AppEngineStandardFacet.installAllAppEngineRuntimes(facetedProject, true /* force */, monitor);
  }

}
