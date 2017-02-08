package com.google.cloud.tools.eclipse.projectselector;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ProjectSelectorContentProvider implements IStructuredContentProvider {
  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  }

  @Override
  public void dispose() {
  }

  @Override
  public Object[] getElements(Object inputElement) {
    return new GcpProject[]{ new GcpProject("project name", "project-id-123"),
        new GcpProject("project name", "project-id-234"),
        new GcpProject("project name", "project-id-345"),
        new GcpProject("project name", "project-id-456"),
        new GcpProject("project name", "project-id-567"),
        new GcpProject("project name", "project-id-678"),
        new GcpProject("project name", "project-id-789"),
        new GcpProject("project name", "project-id-890"),
        new GcpProject("project name", "project-id-901"),
        new GcpProject("project name", "project-id-012")};
  }
}