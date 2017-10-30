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

package com.google.cloud.tools.eclipse.appengine.compat.cte13;

import com.google.cloud.tools.eclipse.appengine.compat.Messages;
import com.google.cloud.tools.eclipse.ui.util.ProjectFromSelectionHelper;
import java.text.MessageFormat;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

public class UpdateCloudToolsEclipseProjectHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    final IProject project = ProjectFromSelectionHelper.getFirstProject(event);
    if (!CloudToolsEclipseProjectUpdater.hasOldContainers(project)) {
      throw new ExecutionException(Messages.getString("project.appears.up.to.date")); //$NON-NLS-1$
    }
    Job updateJob = new WorkspaceJob(MessageFormat.format(Messages.getString("updating.project"), project.getName())) { //$NON-NLS-1$
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        return CloudToolsEclipseProjectUpdater.updateProject(project, SubMonitor.convert(monitor));
      }
    };
    updateJob.setRule(project.getWorkspace().getRoot());
    updateJob.setUser(true);
    updateJob.schedule();
    return null;
  }

}
