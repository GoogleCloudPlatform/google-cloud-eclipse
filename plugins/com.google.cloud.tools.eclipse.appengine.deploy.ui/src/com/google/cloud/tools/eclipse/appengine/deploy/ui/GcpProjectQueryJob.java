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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Generic {@link Job} that queries GCP projects of given {@link Credential} through
 * {@link ProjectRepository} and updates {@link ProjectSelector} asynchronously.
 */
public class GcpProjectQueryJob extends Job {

  private final Credential credential;
  private final ProjectRepository projectRepository;
  private final ProjectSelector projectSelector;
  private final Predicate<Job> isLatestQueryJob;

  /**
   * @param projectRepository {@link ProjectRepository#getProjects} must be thread-safe
   * @param isLatestQueryJob predicate that lazily determines if this job is the latest query job,
   *     which determines if the job should update {@link ProjectSelector} or die silently. This
   *     predicate is executed in the UI context
   */
  GcpProjectQueryJob(Credential credential, ProjectRepository projectRepository,
      ProjectSelector projectSelector, Predicate<Job> isLatestQueryJob) {
    super("Google Cloud Platform Projects Query Job");
    this.credential = Preconditions.checkNotNull(credential);
    this.projectRepository = Preconditions.checkNotNull(projectRepository);
    this.projectSelector = Preconditions.checkNotNull(projectSelector);
    this.isLatestQueryJob = Preconditions.checkNotNull(isLatestQueryJob);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      final Job thisJob = this;
      final List<GcpProject> projects = projectRepository.getProjects(credential);

      // The selector may have been disposed (i.e., dialog closed); check it in the UI thread.
      projectSelector.getDisplay().syncExec(new Runnable() {
        @Override
        public void run() {
          if (!projectSelector.getShell().isDisposed()
              && isLatestQueryJob.apply(thisJob) /* intentionally checking in UI context */) {
            projectSelector.setProjects(projects);
          }
        }
      });
      return Status.OK_STATUS;
    } catch (ProjectRepositoryException ex) {
      return StatusUtil.error(this,
          Messages.getString("projectselector.retrieveproject.error.message"), ex);
    }
  }
}
