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

package com.google.cloud.tools.eclipse.appengine.newproject;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.j2ee.refactor.listeners.J2EEElementChangedListener;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;

/**
 * Removes entries from WTP's deployment assembly whose source path matches a given path.
 */
// Intended to be used by our wizards during project creation. See
// https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1997#issuecomment-310707483
class DeployAssemblyEntryRemoveJob extends Job {

  private final IProject project;
  private final IPath sourcePath;

  DeployAssemblyEntryRemoveJob(IProject project, IPath sourcePath) {
    super(Messages.getString("deploy.assembly.test.source.remove.job")); //$NON-NLS-1$
    this.project = project;
    this.sourcePath = sourcePath;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1997#issuecomment-310707483
      getJobManager().join(J2EEElementChangedListener.PROJECT_COMPONENT_UPDATE_JOB_FAMILY, monitor);

      IVirtualComponent component = ComponentCore.createComponent(project);
      if (component != null && component.exists()) {
        IVirtualFolder rootFolder = component.getRootFolder();
        // Removes an entry in ".settings/org.eclipse.wst.common.component".
        rootFolder.removeLink(sourcePath, IVirtualFolder.FORCE, new NullProgressMonitor());
      }
      return Status.OK_STATUS;
    } catch (OperationCanceledException | InterruptedException ex) {
      return Status.CANCEL_STATUS;
    } catch (CoreException ex) {
      return StatusUtil.error(this, "failed to modify deploy assembly", ex);
    }
  }

}
