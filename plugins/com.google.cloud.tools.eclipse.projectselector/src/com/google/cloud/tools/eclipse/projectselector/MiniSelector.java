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
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.ui.util.DisplayExecutor;
import com.google.cloud.tools.eclipse.util.jobs.FuturisticJob;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A small widget for selecting projects.
 */
public class MiniSelector implements ISelectionProvider {
  private static final Logger logger = Logger.getLogger(MiniSelector.class.getName());
  private static final GcpProject[] EMPTY_PROJECTS = new GcpProject[0];

  private final ProjectRepository projectRepository;

  private Executor displayExecutor;
  private ComboViewer comboViewer;
  private FetchProjectsJob fetchProjectsJob;

  /** Stashed projectID to be selected when fetch-project-list completes. */
  private String toBeSelectedProjectId;

  public MiniSelector(Composite container) {
    projectRepository = new ProjectRepository();
    create(container);
  }

  private void create(Composite parent) {
    displayExecutor = DisplayExecutor.create(parent.getDisplay());
    comboViewer = new ComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
    comboViewer.setComparator(new ViewerComparator());
    comboViewer.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object element) {
        if (element instanceof GcpProject) {
          GcpProject project = (GcpProject) element;
          return project.getName() + " (" + project.getId() + ")";
        }
        return super.getText(element);
      }
    });
    comboViewer.setContentProvider(ArrayContentProvider.getInstance());
    comboViewer.setInput(EMPTY_PROJECTS);
    parent.addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent event) {
        cancelFetch();
      }
    });

    fetch();
  }

  public Control getControl() {
    return comboViewer.getControl();
  }

  /**
   * Set whether the controls are enabled or not.
   */
  public void setEnabled(boolean enabled) {
    comboViewer.getControl().setEnabled(enabled);
  }

  public Credential getCredential() {
    return GoogleApiFactory.INSTANCE.getCredential().orElse(null);
  }

  /**
   * Used to re-fetch the projects in case a credential has changed
   */
  public void updateProjectsList() {
    fetch();
  }

  /**
   * Fetch and update the projects list, preserving the current selection.
   */
  private void fetch() {
    toBeSelectedProjectId = getProjectId(); // save currently selected project ID
    comboViewer.setInput(EMPTY_PROJECTS);

    cancelFetch();
    if (!GoogleApiFactory.INSTANCE.getCredential().isPresent()) {
      logger.log(Level.WARNING, "Tried to fetch() projects without credentials set");
      return;
    }

    fetchProjectsJob = new FetchProjectsJob();
    fetchProjectsJob.onSuccess(
        displayExecutor,
        projects -> {
          comboViewer.setInput(projects);
          setProject(toBeSelectedProjectId);
        });
    // maybe this should be shown to the user?
    fetchProjectsJob.onError(
        MoreExecutors.directExecutor(),
        ex -> logger.log(Level.SEVERE, "Unable to fetch project list", ex));
    fetchProjectsJob.schedule();
  }

  private void cancelFetch() {
    if (fetchProjectsJob != null) {
      fetchProjectsJob.abandon();
      fetchProjectsJob = null;
    }
  }

  /**
   * Return the currently selected project or {@code null} if none selected.
   */
  public GcpProject getProject() {
    IStructuredSelection selection = comboViewer.getStructuredSelection();
    return selection.isEmpty() ? null : (GcpProject) selection.getFirstElement();
  }

  /**
   * Return the currently selected project ID or {@code ""} if none selected.
   */
  public String getProjectId() {
    GcpProject selected = getProject();
    return selected != null ? selected.getId() : "";
  }

  /**
   * Set the currently selected project by ID. The selection may not take effect immediately if the
   * project-list is still being fetched.
   * 
   * @param projectId may be {@code null} or {@code ""} to deselect
   */
  public void setProject(final String projectId) {
    Preconditions.checkState(comboViewer.getInput() instanceof GcpProject[]);
    // if there's a fetch in progress, then we override the result that should be shown
    toBeSelectedProjectId = projectId;
    if (Strings.isNullOrEmpty(projectId)) {
      comboViewer.setSelection(StructuredSelection.EMPTY);
    } else {
      for (GcpProject project : (GcpProject[]) comboViewer.getInput()) {
        if (project.getId().equals(projectId)) {
          comboViewer.setSelection(new StructuredSelection(project));
        }
      }
    }
  }

  @Override
  public IStructuredSelection getSelection() {
    return comboViewer.getStructuredSelection();
  }

  @Override
  public void setSelection(ISelection selection) {
    comboViewer.setSelection(selection);
  }

  @Override
  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    comboViewer.addPostSelectionChangedListener(listener);
  }

  @Override
  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    comboViewer.removePostSelectionChangedListener(listener);
  }

  @VisibleForTesting
  public void join() throws InterruptedException {
    if (fetchProjectsJob != null) {
      fetchProjectsJob.join();
    }
  }

  /**
   * Simple job for fetching projects accessible to the current account.
   */
  private class FetchProjectsJob extends FuturisticJob<GcpProject[]> {
    private final Credential credential; // the credential used for fetch

    public FetchProjectsJob() {
      super("Determining accessible projects");
      credential = GoogleApiFactory.INSTANCE.getCredential().orElse(null);
    }

    @Override
    protected GcpProject[] compute(IProgressMonitor monitor) throws Exception {
      List<GcpProject> projects = projectRepository.getProjects();
      return projects.toArray(new GcpProject[projects.size()]);
    }

    @Override
    protected boolean isStale() {
      // check if the MiniSelector's credential has changed
      return credential != GoogleApiFactory.INSTANCE.getCredential().orElse(null);
    }
  }
}
