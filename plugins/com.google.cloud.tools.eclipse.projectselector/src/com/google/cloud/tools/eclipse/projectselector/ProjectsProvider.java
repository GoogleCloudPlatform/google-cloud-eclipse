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
import com.google.cloud.tools.eclipse.util.jobs.FuturisticJob;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.logging.Level;
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
  private static final Object[] EMPTY_OBJECTS = new Object[0];

  // todo better than reusing Predicate?
  public interface Callback<T> {
    void resolved(T value);
  }

  private final ProjectRepository projectRepository;
  private Executor displayExecutor;
  private Viewer viewer;
  private Credential credential; // the input
  private FetchProjectsJob fetchProjectsJob;

  public ProjectsProvider(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  @Override
  public void dispose() {
    cancel();
  }

  private ListenableFuture<GcpProject[]> query() {
    if (fetchProjectsJob != null) {
      fetchProjectsJob.cancel();
    }
    fetchProjectsJob = new FetchProjectsJob();
    fetchProjectsJob.schedule();
    return fetchProjectsJob.getFuture();
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    this.viewer = viewer;
    this.displayExecutor = DisplayExecutor.create(viewer.getControl().getDisplay());
    if (newInput instanceof Credential && !viewer.getControl().isDisposed()) {
      this.credential = (Credential) newInput;
      query().addListener(new Runnable() {
        @Override
        public void run() {
          if (!ProjectsProvider.this.viewer.getControl().isDisposed()) {
            ProjectsProvider.this.viewer.refresh();
          }
        }
      }, displayExecutor);
    } else {
      cancel();
    }
  }

  @Override
  public Object[] getElements(Object inputElement) {
    if (fetchProjectsJob != null) {
      Future<GcpProject[]> projectsFuture = fetchProjectsJob.getFuture();
      if (projectsFuture != null && projectsFuture.isDone()) {
        try {
          return projectsFuture.get();
        } catch (InterruptedException | ExecutionException ex) {
          logger.log(Level.WARNING, "Error retrieving projects list", ex);
        }
      }
    }
    return EMPTY_OBJECTS;
  }

  /**
   * Execute the given callback once a project is resolved with the given ID. Does nothing if the
   * project is not resolved.
   */
  public void resolve(final String projectId, final Callback<GcpProject> callback) {
    // since this happens after the inputChanged(), we should always happen after the
    // viewer is refreshed
    if (fetchProjectsJob != null) {
      final ListenableFuture<GcpProject[]> projectsFuture = fetchProjectsJob.getFuture();
      projectsFuture.addListener(new Runnable() {
        @Override
        public void run() {
          try {
            for (GcpProject project : projectsFuture.get()) {
              if (projectId.equals(project.getId())) {
                callback.resolved(project);
              }
            }
          } catch (InterruptedException | ExecutionException ex) {
            logger.warning("Unable to fetch project list");
          }
        }
      }, MoreExecutors.directExecutor());
    }
  }

  private void cancel() {
    if (fetchProjectsJob != null) {
      fetchProjectsJob.cancel();
    }
  }

  /**
   * Simple job for fetching projects accessible to the current account.
   */
  private class FetchProjectsJob extends FuturisticJob<GcpProject[]> {
    private final Credential credential;

    public FetchProjectsJob() {
      super("Determining accessible projects");
      this.credential = ProjectsProvider.this.credential;
    }

    @Override
    protected GcpProject[] compute(IProgressMonitor monitor) throws Exception {
      List<GcpProject> projects = projectRepository.getProjects(credential);
      return projects.toArray(new GcpProject[projects.size()]);
    }

    @Override
    protected boolean isStale() {
      return this.credential != ProjectsProvider.this.credential;
    }
  }
}
