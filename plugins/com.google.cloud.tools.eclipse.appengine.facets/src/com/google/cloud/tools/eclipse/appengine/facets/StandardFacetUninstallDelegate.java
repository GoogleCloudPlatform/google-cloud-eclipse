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
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

public class StandardFacetUninstallDelegate implements IDelegate {

  @Override
  public void execute(IProject project, IProjectFacetVersion version, Object config,
      IProgressMonitor monitor) throws CoreException {
    uninstallAppEngineRuntimes(project);
  }

  /**
   * Removes all the App Engine server runtimes from the list of targeted runtimes for
   * <code>project</code>.
   */
  private void uninstallAppEngineRuntimes(final IProject project) {
    // Modifying targeted runtimes while installing/uninstalling facets is not allowed,
    // so schedule a job as a workaround.
    Job uninstallJob = new Job("Uninstall App Engine runtimes in " + project.getName()) {

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          IFacetedProject facetedProject = ProjectFacetsManager.create(project);
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
    };
    uninstallJob.schedule();
  }

}
