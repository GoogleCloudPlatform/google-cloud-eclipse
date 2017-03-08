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

import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.ErrorDialogErrorHandler;
import com.google.common.base.Strings;
import java.util.List;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

public class ProjectSelector extends Composite {

  private final TableViewer tableViewer;
  private final WritableList input;
  private Link statusLink;

  public ProjectSelector(Composite parent) {
    super(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().numColumns(2).applyTo(this);

    Composite tableComposite = new Composite(this, SWT.NONE);
    TableColumnLayout tableColumnLayout = new TableColumnLayout();
    tableComposite.setLayout(tableColumnLayout);
    GridDataFactory.fillDefaults().grab(true, true).applyTo(tableComposite);

    tableViewer = new TableViewer(tableComposite, SWT.SINGLE | SWT.BORDER);
    createColumns(tableColumnLayout);
    tableViewer.getTable().setHeaderVisible(true);
    input = WritableList.withElementType(GcpProject.class);
    ViewerSupport.bind(tableViewer,
                       input,
                       PojoProperties.values(new String[]{ "name", //$NON-NLS-1$
                                                           "id" })); //$NON-NLS-1$
    tableViewer.setComparator(new ViewerComparator());

    statusLink = new Link(this, SWT.NONE);
    statusLink.addSelectionListener(
        new OpenUriSelectionListener(new ErrorDialogErrorHandler(getShell())));
    statusLink.setText("");
    GridDataFactory.fillDefaults().span(2, 1).applyTo(statusLink);
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
    clearStatusLink(); // otherwise revealing selection is off sometimes
    if (projects != null) {
      input.addAll(projects);
    }
    tableViewer.setSelection(selection);
  }

  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    tableViewer.addPostSelectionChangedListener(listener);
  }

  public void removeSelectionChangedListener(ISelectionChangedListener listener) {
    tableViewer.removePostSelectionChangedListener(listener);
  }

  public void setStatusLink(String linkText, String tooltip) {
    statusLink.setText(linkText);
    setTooltip(tooltip);
    boolean hide = Strings.isNullOrEmpty(linkText);
    ((GridData) statusLink.getLayoutData()).exclude = hide;
    statusLink.setVisible(!hide);
    layout();
    IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
    if (!selection.isEmpty()) {
      tableViewer.reveal(selection.getFirstElement());
    }
  }

  private void setTooltip(String tooltip) {
    if (Strings.isNullOrEmpty(tooltip)) {
      statusLink.setToolTipText(null);
    } else {
      statusLink.setToolTipText(tooltip);
    }
  }

  public void clearStatusLink() {
    setStatusLink("", "");
  }
}
