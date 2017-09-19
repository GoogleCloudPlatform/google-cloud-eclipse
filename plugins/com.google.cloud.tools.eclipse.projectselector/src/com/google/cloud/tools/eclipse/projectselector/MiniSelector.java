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
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.ui.util.DisplayExecutor;
import com.google.cloud.tools.eclipse.util.jobs.Consumer;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A small widget for selecting projects.
 */
public class MiniSelector implements ISelectionProvider {
  private static final Logger logger = Logger.getLogger(MiniSelector.class.getName());

  private final ProjectRepository projectRepository;
  private Executor displayExecutor;
  private ComboViewer comboViewer;
  private ProjectsProvider projectsProvider;
  private Credential credential;


  public MiniSelector(Composite container, IGoogleApiFactory apiFactory) {
    this(container, apiFactory, null);
  }

  public MiniSelector(Composite container, IGoogleApiFactory apiFactory, Credential credential) {
    this.credential = credential;
    this.projectRepository = new ProjectRepository(apiFactory);
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
    projectsProvider = new ProjectsProvider(projectRepository);
    comboViewer.setContentProvider(projectsProvider);
    comboViewer.setInput(credential);
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
    return credential;
  }

  public void setCredential(Credential credential) {
    // try to preserve the selection if possible
    GcpProject selected = getProject();
    this.credential = credential;
    comboViewer.setInput(credential);
    if (selected != null) {
      setProject(selected.getId());
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

  /**
   * Return the currently selected project or {@code null} if none selected.
   */
  public GcpProject getProject() {
    IStructuredSelection selection = comboViewer.getStructuredSelection();
    return selection.isEmpty() ? null : (GcpProject) selection.getFirstElement();
  }

  /**
   * Set the currently selected project.
   */
  public void setProject(GcpProject project) {
    setProject(project.getId());
  }

  /**
   * Set the currently selected project by ID; return the project or {@code null} if no project by
   * that ID.
   */
  public void setProject(final String projectId) {
    projectsProvider.resolve(projectId, displayExecutor, new Consumer<GcpProject>() {
      public void accept(final GcpProject resolvedProject) {
        comboViewer.setSelection(new StructuredSelection(resolvedProject));
      }
    });
  }
}
