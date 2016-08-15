/*******************************************************************************
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.facets;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.common.project.facet.core.events.IPrimaryRuntimeChangedEvent;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

/**
 * Listens for events where the App Engine runtime has been made the primary
 * runtime for a project. Installs the App Engine facet if necessary.
 */
public class AppEngineRuntimeChangeListener implements IFacetedProjectListener {

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
    if (AppEngineStandardFacet.hasAppEngineFacet(facetedProject)) {
      return;
    }

    // Add the App Engine facet
    IProject project = facetedProject.getProject();
    Job addFacetJob = new Job("Add App Engine facet to " + project.getName()) {

      @Override
      protected IStatus run(IProgressMonitor monitor) {

        IStatus installStatus = Status.OK_STATUS;

        try {
          AppEngineStandardFacet.installAppEngineFacet(facetedProject, false /* installDependentFacets */, monitor);
          return installStatus;
        } catch (CoreException ex1) {
          // Displays missing constraints that prevented facet installation
          installStatus = ex1.getStatus();

          // Remove App Engine as primary runtime
          try {
            facetedProject.removeTargetedRuntime(newRuntime, monitor);
            return installStatus;
          } catch (CoreException ex2) {
            MultiStatus multiStatus;
            if (installStatus instanceof MultiStatus) {
              multiStatus = (MultiStatus) installStatus;
            } else {
              multiStatus = new MultiStatus(installStatus.getPlugin(), installStatus.getCode(),
                  installStatus.getMessage(), installStatus.getException());
            }
            multiStatus.merge(ex2.getStatus());
            return multiStatus;
          }
        }

      }

    };
    addFacetJob.schedule();
  }

}
