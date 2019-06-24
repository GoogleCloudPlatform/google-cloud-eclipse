/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.standard.java8;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineConfigurationUtil;
import com.google.cloud.tools.eclipse.ui.util.ProjectFromSelectionHelper;
import com.google.common.base.Preconditions;
import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.j2ee.refactor.listeners.J2EEElementChangedListener;

public class ConvertToJava8RuntimeHandler extends AbstractHandler {

  private static final Path APPENGINE_DESCRIPTOR = new Path("appengine-web.xml"); //$NON-NLS-1$

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    List<IProject> projects = ProjectFromSelectionHelper.getProjects(event);
    Preconditions.checkArgument(!projects.isEmpty());
    Job updateJob = new WorkspaceJob(Messages.getString("reconfiguringToJava8")) { // $NON-NLS-1$
          @Override
          public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            SubMonitor progress = SubMonitor.convert(monitor, projects.size());
            for (IProject project : projects) {
              progress.subTask(
                  Messages.getString("reconfiguringProject", project.getName())); // $NON-NLS-1$
              IFile appEngineWebXml =
                  AppEngineConfigurationUtil.findConfigurationFile(project, APPENGINE_DESCRIPTOR);
              if (appEngineWebXml != null) {
                // add the <runtime> and the rest should be handled for us
                AppEngineDescriptorTransform.addJava8Runtime(appEngineWebXml);
              }
              progress.worked(1);
            }
            return Status.OK_STATUS;
          }

          @Override
          public boolean belongsTo(Object family) {
            return super.belongsTo(family)
                || J2EEElementChangedListener.PROJECT_COMPONENT_UPDATE_JOB_FAMILY.equals(family);
          }
        };
    updateJob.setRule(getWorkspaceRoot());
    updateJob.setUser(true);
    updateJob.schedule();
    return null;
  }

  private static IWorkspaceRoot getWorkspaceRoot() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    return workspace.getRoot();
  }
}
