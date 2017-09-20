/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.projectselector;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.ui.util.DisplayExecutor;
import com.google.cloud.tools.eclipse.util.jobs.Consumer;
import com.google.cloud.tools.eclipse.util.jobs.FuturisticJob;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * A JFace {@link IStructuredContentProvider} for retrieving projects for the {@link Credential}
 * provided as its input.
 */
public class ProjectsProvider implements IStructuredContentProvider {
  private static final Logger logger = Logger.getLogger(ProjectsProvider.class.getName());
  private static final GcpProject[] EMPTY_PROJECTS = new GcpProject[0];

  private final ProjectRepository projectRepository;
  private Executor displayExecutor;
  private Viewer viewer;
  private Credential credential; // the input

  @VisibleForTesting
  FetchProjectsJob fetchProjectsJob;

  public ProjectsProvider(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  @Override
  public void dispose() {
    cancel();
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    this.viewer = viewer;
    this.displayExecutor = DisplayExecutor.create(viewer.getControl().getDisplay());
    new Throwable("ProjectsProvider.inputChanged(): newInput=" + newInput).printStackTrace();
    credential = (Credential) newInput;
    if (fetchProjectsJob != null && !fetchProjectsJob.isStale()) {
      logger.info("ProjectsProvider.inputChanged(): fetchProjectJob is still current; returning");
      return;
    }
    if (fetchProjectsJob != null) {
      logger.info("ProjectsProvider.inputChanged(): fetchProjectsJob.abandon()");
      fetchProjectsJob.abandon();
      fetchProjectsJob = null;
    }
    if (credential != null && viewer.getControl() != null && !viewer.getControl().isDisposed()) {
      fetchProjectsJob = new FetchProjectsJob();
      fetchProjectsJob.onSuccess(displayExecutor, new Runnable() {
        @Override
        public void run() {
          if (!ProjectsProvider.this.viewer.getControl().isDisposed()) {
            logger.info("ProjectsProvider.FetchProjectsJob finished: triggering viewer.refresh()");
            ProjectsProvider.this.viewer.refresh();
          }
        }
      });
      logger.info("ProjectsProvider.inputChanged(): scheduling new FetchProjectsJob");
      fetchProjectsJob.schedule();
    }
  }

  @Override
  public Object[] getElements(Object inputElement) {
    if (fetchProjectsJob != null && inputElement == credential) {
      logger.info("getElements(): returning results");
      return fetchProjectsJob.getComputationResult().or(EMPTY_PROJECTS);
    } else if (fetchProjectsJob == null) {
      logger.warning("getElements(): fetchProjectsJobs is null");
    } else if (inputElement != credential) {
      logger.warning("getElements(): different credential: " + inputElement);
    }
    return EMPTY_PROJECTS;
  }

  /**
   * Execute the given callback once a project is resolved with the given ID. Does nothing if the
   * project is not resolved.
   */
  public void resolve(final String projectId, Executor callbackExecutor,
      final Consumer<GcpProject> callback) {
    // since this happens after the inputChanged(), we should always happen after the
    // viewer is refreshed
    if (fetchProjectsJob != null) {
      fetchProjectsJob.onSuccess(callbackExecutor, new Consumer<GcpProject[]>() {
        @Override
        public void accept(GcpProject[] projects) {
          for (final GcpProject project : projects) {
            if (projectId.equals(project.getId())) {
              logger.info("resolve(): initiating callback: found project " + project);
              callback.accept(project);
              return;
            }
          }
        }
      });
    } else {
      logger.warning("resolve(): no fetchProjectsJob found!");
    }
  }

  private void cancel() {
    if (fetchProjectsJob != null) {
      fetchProjectsJob.abandon();
    }
  }

  /**
   * Simple job for fetching projects accessible to the current account.
   */
  @VisibleForTesting
  class FetchProjectsJob extends FuturisticJob<GcpProject[]> {
    private final Credential credential;

    public FetchProjectsJob() {
      super("Determining accessible projects");
      logger.info("FetchProjectsJob() created");
      this.credential = ProjectsProvider.this.credential;
    }

    @Override
    protected GcpProject[] compute(IProgressMonitor monitor) throws Exception {
      List<GcpProject> projects = projectRepository.getProjects(credential);
      logger.info("FetchProjectsJob: found: " + projects);
      return projects.toArray(new GcpProject[projects.size()]);
    }

    @Override
    protected boolean isStale() {
      return this.credential != ProjectsProvider.this.credential;
    }
  }
}
