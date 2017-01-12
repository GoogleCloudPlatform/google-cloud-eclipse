package com.google.cloud.tools.eclipse.appengine.facets.convert;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.Messages;
import com.google.cloud.tools.eclipse.util.NatureUtils;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;

public class AppEngineStandardProjectConvertJob extends Job {

  private static final String GPE_APP_ENGINE_NATURE_ID =
      "com.google.appengine.eclipse.core.gaeNature";

  private final IFacetedProject facetedProject;

  public AppEngineStandardProjectConvertJob(IFacetedProject facetedProject) {
    super("App Engine Standard Project Conversion Job");
    this.facetedProject = facetedProject;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

    try {
      AppEngineStandardFacet.installAppEngineFacet(facetedProject,
          true /* install Java and Web facets too (safe even if already installed) */,
          subMonitor.newChild(80));
    } catch (CoreException ex) {
      String project = facetedProject.getProject().getName();
      return StatusUtil.error(this, Messages.getString("project.conversion.error", project), ex);
    }

    try {
      NatureUtils.removeNature(facetedProject.getProject(), GPE_APP_ENGINE_NATURE_ID);
    } catch (CoreException ex) {
      // Failed to remove obsolete GPE nature, but it's not critical.
    }
    subMonitor.worked(20);
    return Status.OK_STATUS;
  }
}
