/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.facets;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.j2ee.refactor.listeners.J2EEElementChangedListener;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.common.project.facet.core.events.IPrimaryRuntimeChangedEvent;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

/**
 * Listens for events where the App Engine standard runtime has been made the primary
 * runtime for a project. Installs the App Engine facet if necessary.
 */
public class AppEngineStandardRuntimeChangeListener implements IFacetedProjectListener {

  /** A Job to install out App Engine facet given that we've added a runtime. */
  private static class InstallAppEngineFacetJob extends Job {
    private final IFacetedProject facetedProject;
    private final IRuntime newRuntime;

    private InstallAppEngineFacetJob(IFacetedProject facetedProject,
        IRuntime newRuntime) {
      super(Messages.getString("appengine.add.facet.to.project",
          facetedProject.getProject().getName())); // $NON-NLS$
      this.facetedProject = facetedProject;
      this.newRuntime = newRuntime;
    }

    /**
     * Mark this job as a component update job. Useful for our tests to ensure project configuration
     * is complete.
     */
    @Override
    public boolean belongsTo(Object family) {
      return J2EEElementChangedListener.PROJECT_COMPONENT_UPDATE_JOB_FAMILY.equals(family);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      IStatus installStatus = Status.OK_STATUS;

      try {
        AppEngineStandardFacet.installAppEngineFacet(facetedProject,
            false /* installDependentFacets */, monitor);
        return installStatus;
      } catch (CoreException ex1) {
        // Displays missing constraints that prevented facet installation
        installStatus = ex1.getStatus();

        // Remove App Engine as primary runtime
        try {
          facetedProject.removeTargetedRuntime(newRuntime, monitor);
          return installStatus;
        } catch (CoreException ex2) {
          return StatusUtil.merge(installStatus, ex2.getStatus());
        }
      }
    }
  }

  @Override
  public void handleEvent(IFacetedProjectEvent event) {
    // PRIMARY_RUNTIME_CHANGED occurs in scenarios such as selecting runtimes on the
    // "New Faceted Project" wizard and the "New Dynamic Web Project" wizard.
    // IFacetedProjectEvent.Type.TARGETED_RUNTIMES_CHANGED does not happen
    if (event.getType() != IFacetedProjectEvent.Type.PRIMARY_RUNTIME_CHANGED) {
      return;
    }

    IPrimaryRuntimeChangedEvent runtimeChangeEvent = (IPrimaryRuntimeChangedEvent)event;
    final IRuntime newRuntime = runtimeChangeEvent.getNewPrimaryRuntime();
    if (newRuntime == null) {
      return;
    }

    if (!AppEngineStandardFacet.isAppEngineStandardRuntime(newRuntime)) {
      return;
    }

    // Check if the App Engine facet has been installed in the project
    final IFacetedProject facetedProject = runtimeChangeEvent.getProject();
    if (AppEngineStandardFacet.hasFacet(facetedProject)) {
      return;
    }

    // Add the App Engine facet
    Job addFacetJob = new InstallAppEngineFacetJob(facetedProject, newRuntime);
    addFacetJob.schedule();
  }

}
