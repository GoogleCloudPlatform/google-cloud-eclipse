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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * A JFace {@link IStructuredContentProvider} for retrieving projects for the {@link Credential}
 * provided as its input.
 */
public class ProjectsProvider implements IStructuredContentProvider {
  private static final Logger logger = Logger.getLogger(ProjectsProvider.class.getName());
  private static final Object[] EMPTY_OBJECTS = new Object[0];

  public interface Callback<T> {
    void execute(T value);
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
    return fetchProjectsJob.getProjects();
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
      Future<GcpProject[]> projectsFuture = fetchProjectsJob.getProjects();
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
      final ListenableFuture<GcpProject[]> projectsFuture = fetchProjectsJob.getProjects();
      projectsFuture.addListener(new Runnable() {
        @Override
        public void run() {
          try {
            for (GcpProject project : projectsFuture.get()) {
              if (projectId.equals(project.getId())) {
                callback.execute(project);
              }
            }
          } catch (InterruptedException | ExecutionException ex) {
            logger.warning("Unable to fetch project list");
          }
        }
      }, displayExecutor);

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
  private class FetchProjectsJob extends Job {
    private Credential credential;
    private SettableFuture<GcpProject[]> projectsFuture = SettableFuture.create();

    public FetchProjectsJob() {
      super("Determining accessible projects");
      this.credential = ProjectsProvider.this.credential;
    }

    public ListenableFuture<GcpProject[]> getProjects() {
      return projectsFuture;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      checkCancelled(monitor);
      try {
        List<GcpProject> projects = projectRepository.getProjects(credential);
        checkCancelled(monitor);
        // FIXME: filter projects by criteria
        projectsFuture.set(projects.toArray(new GcpProject[projects.size()]));
      } catch (ProjectRepositoryException ex) {
        checkCancelled(monitor);
        projectsFuture.setException(ex);
      }

      return Status.OK_STATUS;
    }

    private void checkCancelled(IProgressMonitor monitor) {
      if (monitor.isCanceled() || this.credential != ProjectsProvider.this.credential) {
        throw new OperationCanceledException();
      }
    }
  }
}
