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

import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.j2ee.refactor.listeners.J2EEElementChangedListener;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

public class StandardFacetUninstallDelegate implements IDelegate {

  /**
   * Removes all the App Engine server runtimes from the list of targeted runtimes for
   * <code>project</code>.
   */
  private final class UninstallAppEngineRuntimesJob extends Job {
    private final IFacetedProject facetedProject;

    private UninstallAppEngineRuntimesJob(IFacetedProject facetedProject) {
      super(Messages.getString("appengine.remove.runtimes.from.project", // $NON-NLS$
          facetedProject.getProject().getName()));
      this.facetedProject = facetedProject;
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
      try {
        Set<IRuntime> targetedRuntimes = facetedProject.getTargetedRuntimes();

        for (IRuntime targetedRuntime : targetedRuntimes) {
          if (AppEngineStandardFacet.isAppEngineStandardRuntime(targetedRuntime)) {
            facetedProject.removeTargetedRuntime(targetedRuntime, monitor);
          }
        }
        return Status.OK_STATUS;
      } catch (CoreException ex) {
        return ex.getStatus();
      }
    }
  }

  @Override
  public void execute(IProject project, IProjectFacetVersion version, Object config,
      IProgressMonitor monitor) throws CoreException {
    // Modifying targeted runtimes while installing/uninstalling facets is not allowed,
    // so schedule a job as a workaround.
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    Job uninstallJob = new UninstallAppEngineRuntimesJob(facetedProject);
    uninstallJob.schedule();
  }

}
