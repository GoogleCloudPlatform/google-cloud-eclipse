
package com.google.cloud.tools.eclipse.appengine.compat.gpe;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.util.Messages;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;

public class GpeConvertJob extends Job {
  private final IFacetedProject facetedProject;

  public GpeConvertJob(IFacetedProject facetedProject) {
    super("Google Plugin for Eclipse Project Conversion Job");
    this.facetedProject = facetedProject;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

    try {
      // Updating project before installing App Engine facet to avoid
      // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1155.
      GpeMigrator.removeObsoleteGpeRemnants(facetedProject, subMonitor.newChild(20));

      if (!monitor.isCanceled()) {
        AppEngineStandardFacet.installAppEngineFacet(facetedProject,
            true /* install Java and Web facets too (safe even if already installed) */,
            subMonitor.newChild(80));
      }

      return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
    } catch (CoreException ex) {
      String project = facetedProject.getProject().getName();
      return StatusUtil.error(this, Messages.getString("project.conversion.error", project), ex);
    }
  }
}
