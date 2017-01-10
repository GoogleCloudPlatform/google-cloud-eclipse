package com.google.cloud.tools.eclipse.appengine.facets.convert;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.Messages;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;

public class AppEngineStandardProjectConvertJob extends Job {

  private final IFacetedProject facetedProject;

  public AppEngineStandardProjectConvertJob(IFacetedProject facetedProject) {
    super("App Engine Standard Project Conversion Job");
    this.facetedProject = facetedProject;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      AppEngineStandardFacet.installAppEngineFacet(facetedProject,
          true /* install Java and Web facets too (safe even if already installed) */,
          null /* monitor */);
      return Status.OK_STATUS;
    } catch (CoreException ex) {
      String project = facetedProject.getProject().getName();
      return StatusUtil.error(this, Messages.getString("project.conversion.error", project), ex);
    }
  }

}
