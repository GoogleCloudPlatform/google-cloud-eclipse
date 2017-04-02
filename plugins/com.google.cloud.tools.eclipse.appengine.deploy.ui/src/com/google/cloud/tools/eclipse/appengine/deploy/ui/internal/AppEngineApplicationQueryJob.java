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

package com.google.cloud.tools.eclipse.appengine.deploy.ui.internal;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.Messages;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.AppEngine;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.common.base.Predicate;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

public class AppEngineApplicationQueryJob extends Job {

  private final GcpProject project;
  private final Credential credential;
  private final ProjectRepository projectRepository;
  private final ProjectSelector projectSelector;
  private final Predicate<Job> isLatestAppQueryJob;
  private final String createAppLink;
  private final Display display;

  /**
   * @param projectRepository {@link ProjectRepository#getAppEngineApplication} must be thread-safe
   * @param isLatestQueryJob predicate that lazily determines if this job is the latest query job,
   *     which determines if the job should update {@link ProjectSelector} or die silently. This
   *     predicate is executed in the UI context.
   */
  public AppEngineApplicationQueryJob(GcpProject project, Credential credential,
      ProjectRepository projectRepository, ProjectSelector projectSelector, String createAppLink,
      Predicate<Job> isLatestQueryJob) {
    super("Checking GCP project has App Engine Application...");
    this.project = project;
    this.credential = credential;
    this.projectRepository = projectRepository;
    this.projectSelector = projectSelector;
    this.createAppLink = createAppLink;
    this.isLatestAppQueryJob = isLatestQueryJob;
    display = projectSelector.getDisplay();
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      AppEngine appEngine = projectRepository.getAppEngineApplication(credential, project.getId());
      project.setAppEngine(appEngine);

      if (appEngine == AppEngine.NO_APPENGINE_APPLICATION) {
        String statusMessage = Messages.getString(
            "projectselector.missing.appengine.application.link", createAppLink);
        String statusTooltip = createAppLink;
        updateStatus(statusMessage, statusTooltip);
      }
    } catch (ProjectRepositoryException ex) {
      String statusMessage = Messages.getString(
          "projectselector.retrieveapplication.error.message", ex.getLocalizedMessage(), ex);
      updateStatus(statusMessage, null /* statusTooltip */);
    }
    return Status.OK_STATUS;
  }

  private void updateStatus(final String statusMessage, final String statusTooltip) {
    final Job thisJob = this;

    // The selector may have been disposed (i.e., dialog closed); check it in the UI thread.
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        if (!projectSelector.isDisposed()
            && isLatestAppQueryJob.apply(thisJob) /* intentionally checking in UI context */
            // Covers the case where user switches accounts.
            && !projectSelector.getViewer().getSelection().isEmpty()) {
          projectSelector.setStatusLink(statusMessage, statusTooltip);
        }
      }
    });
  }
}
