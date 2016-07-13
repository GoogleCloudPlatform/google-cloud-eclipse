package com.google.cloud.tools.eclipse.appengine.deploy;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.google.common.base.Preconditions;

/**
 * Wraps certain {@link org.eclipse.wst.common.project.facet.core.ProjectFacetsManager} static methods to allow 
 * unit testing of classes that use those methods. 
 */
// TODO this class could probably have a better bundle to live in
public class FacetedProjectHelper {

  /**
   * Wraps {@link org.eclipse.wst.common.project.facet.core.ProjectFacetsManager#create(IProject)}
   */
  public IFacetedProject getFacetedProject(IProject project) throws CoreException {
    Preconditions.checkNotNull(project, "project is null");
    return ProjectFacetsManager.create(project);
  }

  /**
   * Wraps {@link org.eclipse.wst.common.project.facet.core.ProjectFacetsManager#getProjectFacet(String)} and
   * {@link org.eclipse.wst.common.project.facet.core.IFacetedProjectBase#hasProjectFacet(IProjectFacet)}.
   */
  public boolean projectHasFacet(IFacetedProject facetedProject, String facetId) {
    Preconditions.checkNotNull(facetedProject, "facetedProject is null");
    Preconditions.checkNotNull(facetId, "facetId is null");
    Preconditions.checkArgument(!facetId.isEmpty(), "facetId is empty string");
    
    IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(facetId);
    return facetedProject.hasProjectFacet(projectFacet);
  }

}
