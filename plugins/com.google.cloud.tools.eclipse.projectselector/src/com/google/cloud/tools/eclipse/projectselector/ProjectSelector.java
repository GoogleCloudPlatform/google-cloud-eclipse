package com.google.cloud.tools.eclipse.projectselector;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class ProjectSelector {

  private TableViewer tableViewer;

  public ProjectSelector(Composite parent) {
    tableViewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    TableViewerColumn nameColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
    nameColumn.getColumn().setText("Name");
    nameColumn.getColumn().setWidth(200);
    TableViewerColumn idColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
    idColumn.getColumn().setWidth(200);
    idColumn.getColumn().setText("ID");
    tableViewer.getTable().setHeaderVisible(true);
    tableViewer.setContentProvider(new ProjectSelectorContentProvider());
    tableViewer.setLabelProvider(new ProjectSelectorLabelProvider());
    tableViewer.setComparator(new ViewerComparator());
    tableViewer.setInput(tableViewer);
  }

  public void setLayoutData(Object gridData) {
    tableViewer.getTable().setLayoutData(gridData);
  }

}
