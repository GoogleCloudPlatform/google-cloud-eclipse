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
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.eclipse.wst.common.project.facet.core.runtime.RuntimeManager;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
* Utility to make a new Eclipse project with the App Engine Standard facets in the workspace.  
*/
class CreateAppEngineStandardWtpProject extends WorkspaceModifyOperation {
  
  private static final String DEFAULT_RUNTIME_ID = "com.google.cloud.tools.eclipse.runtime.custom";
  private static final String DEFAULT_RUNTIME_NAME = "App Engine (Custom)";
  private static final String APPENGINE_FACET_ID = "com.google.cloud.tools.eclipse.appengine.facet";

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
    
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject newProject = config.getProject();
    URI location = config.getEclipseProjectLocationUri();

    String name = newProject.getName();
    final IProjectDescription description = workspace.newProjectDescription(name);
    description.setLocationURI(location);
    
    CreateProjectOperation operation = new CreateProjectOperation(
        description, "Creating new App Engine Project");
    try {
      operation.execute(progress.newChild(20), uiInfoAdapter);
      
      IFacetedProject facetedProject = ProjectFacetsManager.create(
          newProject, true, progress.newChild(20));
      
      installJavaFacet(facetedProject, progress.newChild(10));
      
      CodeTemplates.materialize(newProject, config, progress.newChild(20));
      
      installWebFacet(facetedProject, progress.newChild(10));
      
      // must happen after other two facets because the appengine facet requires them
      installAppEngineFacet(facetedProject, progress.newChild(10));
      installAppEngineRuntime(facetedProject, progress.newChild(20));
    } catch (ExecutionException ex) {
      throw new InvocationTargetException(ex, ex.getMessage());
    } finally {
      progress.done();
    }
  }

  private void installAppEngineFacet(IFacetedProject facetedProject, IProgressMonitor monitor) throws CoreException {
    IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
    IProjectFacet appEngineFacet = ProjectFacetsManager.getProjectFacet(APPENGINE_FACET_ID);
    IProjectFacetVersion appEngineFacetVersion = appEngineFacet.getVersion("1");
    workingCopy.addProjectFacet(appEngineFacetVersion);
    workingCopy.commitChanges(monitor);
  }

  static void installJavaFacet(IFacetedProject facetedProject, IProgressMonitor monitor) 
      throws CoreException {
    JavaFacetInstallConfig javaConfig = new JavaFacetInstallConfig();
    List<IPath> sourcePaths = new ArrayList<>();
    sourcePaths.add(new Path("src/main/java"));
    sourcePaths.add(new Path("src/test/java"));
    javaConfig.setSourceFolders(sourcePaths);
    facetedProject.installProjectFacet(JavaFacet.VERSION_1_7, javaConfig, monitor);
  }

  static void installWebFacet(IFacetedProject facetedProject, IProgressMonitor monitor)
      throws CoreException {
    IDataModel webModel = DataModelFactory.createDataModel(new WebFacetInstallDataModelProvider());
    webModel.setBooleanProperty(IJ2EEModuleFacetInstallDataModelProperties.ADD_TO_EAR, false);
    webModel.setBooleanProperty(IJ2EEFacetInstallDataModelProperties.GENERATE_DD, false);
    webModel.setBooleanProperty(IWebFacetInstallDataModelProperties.INSTALL_WEB_LIBRARY, false);
    webModel.setStringProperty(IWebFacetInstallDataModelProperties.CONFIG_FOLDER, "src/main/webapp");
    facetedProject.installProjectFacet(WebFacetUtils.WEB_25, webModel, monitor);
  }

  void installAppEngineRuntime(IFacetedProject project, IProgressMonitor monitor)
      throws CoreException {
    Set<IProjectFacetVersion> facets = new HashSet<>();
    facets.add(WebFacetUtils.WEB_25);
    Set<IRuntime> runtimes = RuntimeManager.getRuntimes(facets);
    project.setTargetedRuntimes(runtimes, monitor);
    
    if (RuntimeManager.isRuntimeDefined(DEFAULT_RUNTIME_NAME)) {
      IRuntime appEngineRuntime = RuntimeManager.getRuntime(DEFAULT_RUNTIME_NAME);
      project.setPrimaryRuntime(appEngineRuntime, monitor);
    } else { // Create a new App Engine runtime
      IRuntimeType appEngineRuntimeType =
          ServerCore.findRuntimeType(DEFAULT_RUNTIME_ID);
      if (appEngineRuntimeType == null) {
        throw new NullPointerException("Could not find " + DEFAULT_RUNTIME_NAME + " runtime type");
      }

      IRuntimeWorkingCopy appEngineRuntimeWorkingCopy 
          = appEngineRuntimeType.createRuntime(null, monitor);
      org.eclipse.wst.server.core.IRuntime appEngineServerRuntime 
          = appEngineRuntimeWorkingCopy.save(true, monitor);
      IRuntime appEngineFacetRuntime = FacetUtil.getRuntime(appEngineServerRuntime);
      if (appEngineFacetRuntime == null) {
        throw new NullPointerException("Could not locate App Engine facet runtime");
      }
  
      project.addTargetedRuntime(appEngineFacetRuntime, monitor);
      project.setPrimaryRuntime(appEngineFacetRuntime, monitor);
    }
  }
}
