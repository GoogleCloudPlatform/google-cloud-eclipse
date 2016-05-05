package com.google.cloud.tools.eclipse.appengine.newproject;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.common.project.facet.core.JavaFacetInstallConfig;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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
    
    // todo just use getproject().getLocationUri()
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
      JavaFacetInstallConfig javaConfig = new JavaFacetInstallConfig();
      List<IPath> sourcePaths = new ArrayList<>();
      sourcePaths.add(new Path("src/main/java"));
      sourcePaths.add(new Path("src/test/java"));
      javaConfig.setSourceFolders(sourcePaths);
      facetedProject.installProjectFacet(JavaFacet.VERSION_1_7, javaConfig, monitor);

      CodeTemplates.materialize(newProject, config, progress.newChild(40));
      
    } catch (ExecutionException ex) {
      throw new InvocationTargetException(ex);
    } finally {
      progress.done();
    }
  }

}
