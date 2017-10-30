/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.facets.convert;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.Messages;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;

public class AppEngineStandardProjectConvertJob extends Job {

  protected final IFacetedProject facetedProject;

  protected AppEngineStandardProjectConvertJob(String description, IFacetedProject facetedProject) {
    super(description);
    this.facetedProject = facetedProject;
  }

  public AppEngineStandardProjectConvertJob(IFacetedProject facetedProject) {
    this("App Engine Standard Project Conversion Job", facetedProject);
  }

  @Override
  protected final IStatus run(IProgressMonitor monitor) {
    String projectName = facetedProject.getProject().getName();
    MultiStatus status =
        StatusUtil.multi(this, Messages.getString("project.conversion.error", projectName));
    convert(status, monitor);
    return monitor.isCanceled() ? Status.CANCEL_STATUS : status;
  }

  /**
   * Perform the conversion. Any errors should be accumulated in {@code status}.
   */
  protected void convert(MultiStatus status, IProgressMonitor monitor) {
    if (!monitor.isCanceled()) {
      try {
        /* Install Java and Web facets too (safe even if already installed) */
        boolean installDependentFacets = true;
        AppEngineStandardFacet.installAppEngineFacet(facetedProject, installDependentFacets,
            monitor);
      } catch (CoreException ex) {
        status.add(StatusUtil.error(this, "Unable to install App Engine Standard facet", ex));
      }
    }
  }
}
