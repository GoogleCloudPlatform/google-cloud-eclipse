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

import java.util.List;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class ProjectSelector extends Composite {

  private final TableViewer tableViewer;
  private final WritableList input;

  public ProjectSelector(Composite parent) {
    super(parent, SWT.NONE);
    tableViewer = new TableViewer(this, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    TableColumnLayout tableColumnLayout = new TableColumnLayout();
    setLayout(tableColumnLayout);
    createColumns(tableColumnLayout);

    tableViewer.getTable().setHeaderVisible(true);
    input = WritableList.withElementType(GcpProject.class);
    ViewerSupport.bind(tableViewer,
                       input,
                       PojoProperties.values(new String[]{ "name", //$NON-NLS-1$
                                                           "id" })); //$NON-NLS-1$
    tableViewer.setComparator(new ViewerComparator());
  }

  private void createColumns(TableColumnLayout tableColumnLayout) {
    TableViewerColumn nameColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
    nameColumn.getColumn().setText(Messages.getString("projectselector.header.name")); //$NON-NLS-1$
    tableColumnLayout.setColumnData(nameColumn.getColumn(), new ColumnWeightData(1, 200));

    TableViewerColumn idColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
    idColumn.getColumn().setWidth(200);
    idColumn.getColumn().setText(Messages.getString("projectselector.header.id")); //$NON-NLS-1$
    tableColumnLayout.setColumnData(idColumn.getColumn(), new ColumnWeightData(1, 200));
  }

  public TableViewer getViewer() {
    return tableViewer;
  }

  public void setProjects(List<GcpProject> projects) {
    ISelection selection = tableViewer.getSelection();
    input.clear();
    if (projects != null) {
      input.addAll(projects);
    }
    tableViewer.setSelection(selection);
  }
}
