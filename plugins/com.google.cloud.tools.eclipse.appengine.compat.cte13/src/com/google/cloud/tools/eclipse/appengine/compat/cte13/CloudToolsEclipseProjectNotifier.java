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

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * Looks for projects using the per-library classpath container approach used in versions prior
 * Cloud Tools for Eclipse 1.4, and notifies the user to upgrade.
 */
public class CloudToolsEclipseProjectNotifier implements IStartup {
  private static final Logger logger =
      Logger.getLogger(CloudToolsEclipseProjectNotifier.class.getName());
  private IWorkbench workbench;
  private IWorkspace workspace;

  @Override
  public void earlyStartup() {
    workbench = PlatformUI.getWorkbench();
    workspace = ResourcesPlugin.getWorkspace();

    Job projectUpdater = new WorkspaceJob("Updating projects for Cloud Tools for Eclipse") {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 40);
        progress.subTask("Searching for projects requiring updating");
        Collection<IProject> projects = findCandidates(progress.newChild(10));
        if (projects.isEmpty()) {
          return Status.OK_STATUS;
        }
        projects = promptUser(projects, progress.newChild(5));
        if (projects.isEmpty()) {
          return Status.OK_STATUS;
        }
        progress.subTask("Updating projects");
        return upgradeProjects(projects, progress.newChild(25));
      }
    };
    projectUpdater.setRule(workspace.getRoot());
    projectUpdater.setUser(true);
    projectUpdater.schedule(500);
  }

  /**
   * Scan the current projects to identify those requiring upgrading.
   */
  protected Collection<IProject> findCandidates(SubMonitor progress) {
    List<IProject> projects = new ArrayList<>();

    IProject[] allProjects = workspace.getRoot().getProjects();
    progress.setWorkRemaining(allProjects.length);
    for (IProject project : allProjects) {
      if (CloudToolsEclipseProjectUpdater.hasOldContainers(project)) {
        projects.add(project);
      }
      progress.worked(1);
    }
    Collections.sort(projects, Ordering.usingToString());
    return projects;
  }


  /**
   * Prompt the user to select the projects to upgrade.
   */
  protected Collection<IProject> promptUser(final Collection<IProject> projects,
      SubMonitor progress) {
    Preconditions.checkArgument(!projects.isEmpty(), "no projects specified!"); // $NON-NLS-1$
    progress.setBlocked(StatusUtil.info(this, "Prompting user to upgrade"));
    final boolean[] proceed = new boolean[1];
    workbench.getDisplay().syncExec(new Runnable() {
      public void run() {
        StringBuilder sb = new StringBuilder(
            "The following projects must be updated for Cloud Tools for Eclipse:\n");
        for (IProject project : projects) {
          sb.append("\n    ").append(project.getName());
        }
        sb.append("\n\nUpdate now?");
        proceed[0] =
            MessageDialog.openQuestion(getShell(), "Cloud Tools for Eclipse", sb.toString());
      }
    });
    progress.clearBlocked();
    return proceed[0] ? projects : Collections.<IProject>emptyList();
  }

  /**
   * Find a shell for the projects-require-updating prompt. May return {@code null}.
   */
  protected Shell getShell() {
    if (workbench.getWorkbenchWindowCount() > 0) {
      if (workbench.getActiveWorkbenchWindow() != null) {
        return workbench.getActiveWorkbenchWindow().getShell();
      }
    }
    return workbench.getDisplay().getActiveShell();
  }

  /**
   * Perform the upgrade.
   * 
   */
  protected IStatus upgradeProjects(Collection<IProject> projects, SubMonitor progress) {
    progress.setWorkRemaining(projects.size());
    MultiStatus status = StatusUtil.multi(this, "Updating projects for Cloud Tools for Eclipse");
    for (IProject project : projects) {
      progress.subTask("Updating " + project.getName());
      IStatus result = CloudToolsEclipseProjectUpdater.updateProject(project, progress.newChild(1));
      status.merge(result);
    }
    return status;
  }

}
