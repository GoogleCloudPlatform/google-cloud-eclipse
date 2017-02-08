package com.google.cloud.tools.eclipse.projectselector;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class ProjectSelectorLabelProvider extends LabelProvider implements ITableLabelProvider {

  private static final int COLUMN_INDEX_NAME = 0;
  private static final int COLUMN_INDEX_ID = 1;

  @Override
  public Image getColumnImage(Object element, int columnIndex) {
    return null;
  }

  @Override
  public String getColumnText(Object element, int columnIndex) {
    if (element instanceof GcpProject) {
      GcpProject project = (GcpProject) element;
      switch (columnIndex) {
        case COLUMN_INDEX_NAME:
          return project.getName();
        case COLUMN_INDEX_ID:
          return project.getId();
        default:
          return "";
      }
    } else {
      return element.toString();
    }
  }

  @Override
  public String getText(Object element) {
    if (element instanceof GcpProject) {
      GcpProject project = (GcpProject) element;
      return project.getName();
    } else {
      return super.getText(element);
    }
  }
}