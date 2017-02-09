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

package com.google.cloud.tools.eclipse.projectselector;

import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class ProjectSelector {

  private TableViewer tableViewer;
  private AccountSelector accountSelector;
  private ProjectRepository projectRepository;

  private WritableList input;

  public ProjectSelector(Composite parent, AccountSelector accountSelector) {
    this(parent, accountSelector, new DefaultProjectRepository());
  }

  @VisibleForTesting
  ProjectSelector(Composite parent, AccountSelector accountSelector,
                         ProjectRepository projectRepository) {
    tableViewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    createColumns();

    tableViewer.getTable().setHeaderVisible(true);
    input = WritableList.withElementType(GcpProject.class);
    ViewerSupport.bind(tableViewer,
                       input,
                       PojoProperties.values(new String[]{ "name", //$NON-NLS-1$
                                                           "id" })); //$NON-NLS-1$
    tableViewer.setComparator(new ViewerComparator());

    this.accountSelector = accountSelector;
    this.accountSelector.addSelectionListener(new Runnable() {
      @Override
      public void run() {
        ISelection selection = tableViewer.getSelection();
        input.clear();
        input.addAll(retireveProjects());
        tableViewer.setSelection(selection);
      }
    });

    this.projectRepository = projectRepository;
  }

  private void createColumns() {
    TableViewerColumn nameColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
    nameColumn.getColumn().setText(Messages.getString("projectselector.header.name"));
    nameColumn.getColumn().setWidth(200);
    TableViewerColumn idColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
    idColumn.getColumn().setWidth(200);
    idColumn.getColumn().setText(Messages.getString("projectselector.header.id"));
  }

  private List<GcpProject> retireveProjects() {
    try {
      return projectRepository.getProjects(accountSelector.getSelectedCredential());
    } catch (ProjectRepositoryException ex) {
      ErrorDialog.openError(tableViewer.getControl().getShell(),
                            Messages.getString("projectselector.retrieveproject.error.title"),
                            Messages.getString("projectselector.retrieveproject.error.message",
                                               ex.getLocalizedMessage()),
                            StatusUtil.error(this,
                                             Messages.getString("projectselector.retrieveproject.error.title"),
                                             ex));
      return Collections.emptyList();
    }
  }

  public void setLayoutData(Object gridData) {
    tableViewer.getTable().setLayoutData(gridData);
  }

  public TableViewer getViewer() {
    return tableViewer;
  }

  public GcpProject getProject(String projectId) {
    for (Object element : input) {
      GcpProject project = (GcpProject) element;
      if (project.getId().equals(projectId)) {
        return project;
      }
    }
    return null;
  }
}
