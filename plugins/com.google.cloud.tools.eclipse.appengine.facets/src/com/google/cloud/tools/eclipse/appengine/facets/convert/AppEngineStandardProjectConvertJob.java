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

import com.google.cloud.tools.eclipse.appengine.compat.GpeMigrator;
import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.facets.Messages;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;

public class AppEngineStandardProjectConvertJob extends Job {

  private static final Logger logger =
      Logger.getLogger(AppEngineStandardProjectConvertJob.class.getName());

  private final IFacetedProject facetedProject;

  public AppEngineStandardProjectConvertJob(IFacetedProject facetedProject) {
    super("App Engine Standard Project Conversion Job");
    this.facetedProject = facetedProject;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

    // Updating project before installing App Engine facet to avoid
    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1155.
    try {
      GpeMigrator.removeObsoleteGpeRemnants(facetedProject, subMonitor.newChild(20));
    } catch (CoreException ex) {
      // Failed to remove GPE remnants; live with it.
      subMonitor.setWorkRemaining(20);
      subMonitor.worked(20);
      logger.log(Level.WARNING, "Error while removing GPE remnants", ex);
    }

    try {
      AppEngineStandardFacet.installAppEngineFacet(facetedProject,
          true /* install Java and Web facets too (safe even if already installed) */,
          subMonitor.newChild(80));
      return Status.OK_STATUS;
    } catch (CoreException ex) {
      String project = facetedProject.getProject().getName();
      return StatusUtil.error(this, Messages.getString("project.conversion.error", project), ex);
    }
  }
}
