package com.google.cloud.tools.eclipse.appengine.facets.convert;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.Messages;
import com.google.cloud.tools.eclipse.util.NatureUtils;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

public class AppEngineStandardProjectConvertJob extends Job {

  private static final String GPE_GAE_NATURE_ID = "com.google.appengine.eclipse.core.gaeNature";
  private static final String GPE_WTP_SERVER_RUNTIME = "com.google.appengine.runtime.id1";
  private static final String GPE_WTP_SERVER_RUNTIME_COMPONENT = "com.google.appengine.runtime.id";

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

    removeObsoleteFixtures(facetedProject);
    monitor.worked(20);
    return Status.OK_STATUS;
  }

  @VisibleForTesting
  void removeObsoleteFixtures(IFacetedProject facetedProject) {
    try {
      NatureUtils.removeNature(facetedProject.getProject(), GPE_GAE_NATURE_ID);

      IProjectFacet gpeWtpGaeFacet = ProjectFacetsManager.getProjectFacet("com.google.appengine.facet");
      IProjectFacetVersion gpeWtpGaeFacetVersion = gpeWtpGaeFacet.getVersion("1");

      IProjectFacet gpeWtpGaeEarFacet = ProjectFacetsManager.getProjectFacet("com.google.appengine.facet.ear");

      facetedProject.uninstallProjectFacet(
          gpeWtpGaeFacetVersion, null /* config */, null /* monitor */);

      Set<IRuntime> runtimes = facetedProject.getTargetedRuntimes();
      for (IRuntime runtime : runtimes) {
        if (GPE_WTP_SERVER_RUNTIME.equals(runtime.getProperty("id"))
            || GPE_WTP_SERVER_RUNTIME_COMPONENT.equals(runtime.getProperty("id"))) {
          facetedProject.removeTargetedRuntime(runtime, null /* monitor */);
        }
      }
    } catch (CoreException ex) {
      // Failed to remove obsolete natures and facets. Live with it.
    }
  }
}
