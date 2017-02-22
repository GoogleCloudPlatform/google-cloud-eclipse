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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ProjectSelector extends Composite {

  private final TableViewer tableViewer;
  private final WritableList input;
  private Link statusLink;

  public ProjectSelector(Composite parent) {
    super(parent, SWT.NONE);
    setLayout(new GridLayout(2, false));

    Composite tableComposite = new Composite(this, SWT.NONE);
    TableColumnLayout tableColumnLayout = new TableColumnLayout();
    tableComposite.setLayout(tableColumnLayout);
    GridDataFactory.fillDefaults().grab(true, true).span(1, 2).applyTo(tableComposite);

    tableViewer = new TableViewer(tableComposite, SWT.SINGLE | SWT.BORDER);
    createColumns(tableColumnLayout);
    tableViewer.getTable().setHeaderVisible(true);
    input = WritableList.withElementType(GcpProject.class);
    ViewerSupport.bind(tableViewer,
                       input,
                       PojoProperties.values(new String[]{ "name", //$NON-NLS-1$
                                                           "id" })); //$NON-NLS-1$
    tableViewer.setComparator(new ViewerComparator());

    Button createProjectButton = new Button(this, SWT.NONE);
    createProjectButton.setText(Messages.getString("projectselector.create.newproject"));
    GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(createProjectButton);
    createProjectButton.addSelectionListener(new SelectionListener() {
      
      @Override
      public void widgetSelected(SelectionEvent e) {
        openCreateProjectDialog();
      }
      
      @Override
      public void widgetDefaultSelected(SelectionEvent event) {
        widgetSelected(event);
      }
    });

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

  private void openCreateProjectDialog() {
    new Dialog(this.getShell()) {

      @Override
      protected Control createDialogArea(Composite parent) {
        getShell().setText("Create new GCP project");
        Composite control = (Composite) super.createDialogArea(parent);
        Link link = new Link(control, SWT.NONE);
        link.setText("You can create a new GCP project in the <a>Cloud Console</a>");
        link.addSelectionListener(new SelectionListener() {
          
          @Override
          public void widgetSelected(SelectionEvent e) {
            try {
              PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("https://console.cloud.google.com"));
            } catch (PartInitException | MalformedURLException e1) {
              // TODO Auto-generated catch block
              e1.printStackTrace();
            }
          }
          
          @Override
          public void widgetDefaultSelected(SelectionEvent event) {
            widgetSelected(event);
          }
        });
        return control;
      }
      
    }.open();
    
  }

}
