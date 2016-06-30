package com.google.cloud.tools.eclipse.appengine.newproject;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.jst.server.core.FacetUtil;
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

public class AppEngineFacet {

  public static final String APP_ENGINE_FACET_ID = "com.google.cloud.tools.eclipse.appengine.facet";
  private static final String DEFAULT_RUNTIME_ID = "com.google.cloud.tools.eclipse.runtime.custom";
  private static final String DEFAULT_RUNTIME_NAME = "App Engine Standard (Custom)";
  
  public static void installAppEngineRuntime(IFacetedProject project, IProgressMonitor monitor)
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

  public static void installAppEngineFacet(IFacetedProject facetedProject, IProgressMonitor monitor)
      throws CoreException {
    IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
    IProjectFacet appEngineFacet = ProjectFacetsManager.getProjectFacet(APP_ENGINE_FACET_ID);
    IProjectFacetVersion appEngineFacetVersion = appEngineFacet.getVersion("1");
    workingCopy.addProjectFacet(appEngineFacetVersion);
    workingCopy.commitChanges(monitor);
  }

}
