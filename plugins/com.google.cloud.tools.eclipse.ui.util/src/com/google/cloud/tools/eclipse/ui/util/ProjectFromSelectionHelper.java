package com.google.cloud.tools.eclipse.ui.util;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.util.AdapterUtil;
import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;

public class ProjectFromSelectionHelper {

  private FacetedProjectHelper facetedProjectHelper;
  
  public ProjectFromSelectionHelper(FacetedProjectHelper facetedProjectHelper) {
    this.facetedProjectHelper = facetedProjectHelper;
  }

  public IProject getProject(ExecutionEvent event) throws CoreException, ExecutionException {
    ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      if (structuredSelection.size() == 1) {
        IProject project = AdapterUtil.adapt(structuredSelection.getFirstElement(), IProject.class);
        if (project == null) {
          return null;
        }

        IFacetedProject facetedProject = facetedProjectHelper.getFacetedProject(project);
        if (AppEngineStandardFacet.hasAppEngineFacet(facetedProject)) {
          return project;
        }
      }
    }
    return null;
  }
}
