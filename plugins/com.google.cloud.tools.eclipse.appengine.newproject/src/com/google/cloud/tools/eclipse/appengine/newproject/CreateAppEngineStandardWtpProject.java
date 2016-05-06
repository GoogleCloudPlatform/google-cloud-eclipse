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
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties.FacetDataModelMap;
import org.eclipse.wst.common.componentcore.internal.util.IModuleConstants;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
      
      System.err.println(1);
      IDataModel model = DataModelFactory.createDataModel(IWebFacetInstallDataModelProperties.class);
      System.err.println(2);

      //model.setProperty(IFacetDataModelProperties.FACET_PROJECT_NAME, "WebTest1");

      FacetDataModelMap map = (FacetDataModelMap) model.getProperty(
          IFacetProjectCreationDataModelProperties.FACET_DM_MAP);
      System.err.println(3);

      IDataModel webModel = (IDataModel) map.get("jst.web");
      System.err.println(4);

      webModel.setBooleanProperty(IJ2EEFacetInstallDataModelProperties.GENERATE_DD, false);
      facetedProject.installProjectFacet(WebFacetUtils.WEB_25, webModel, monitor);
      
      
    } catch (ExecutionException ex) {
      throw new InvocationTargetException(ex);
    } finally {
      progress.done();
    }
  }

}
