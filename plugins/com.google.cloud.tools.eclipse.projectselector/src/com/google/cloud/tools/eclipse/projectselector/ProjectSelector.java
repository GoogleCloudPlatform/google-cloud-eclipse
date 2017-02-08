package com.google.cloud.tools.eclipse.projectselector;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class ProjectSelector {

  private TableViewer tableViewer;
  private AccountSelector accountSelector;

  public ProjectSelector(Composite parent, AccountSelector accountSelector) {
    tableViewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    TableViewerColumn nameColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
    nameColumn.getColumn().setText("Name");
    nameColumn.getColumn().setWidth(200);
    TableViewerColumn idColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
    idColumn.getColumn().setWidth(200);
    idColumn.getColumn().setText("ID");
    tableViewer.getTable().setHeaderVisible(true);
    tableViewer.setContentProvider(new ArrayContentProvider());
    tableViewer.setLabelProvider(new ProjectSelectorLabelProvider());
    tableViewer.setComparator(new ViewerComparator());

    this.accountSelector = accountSelector;
    this.accountSelector.addSelectionListener(new Runnable() {
      @Override
      public void run() {
        tableViewer.setInput(queryProjects());
      }
    });
  }

  public void setLayoutData(Object gridData) {
    tableViewer.getTable().setLayoutData(gridData);
  }

  public List<GcpProject> queryProjects() {
    //TODO filter non-active projects
    Credential selectedCredential = accountSelector.getSelectedCredential();
    try {
      if (selectedCredential != null) {
        JsonFactory jsonFactory = new JacksonFactory();
        HttpTransport transport = new NetHttpTransport();
        CloudResourceManager resourceManager = new CloudResourceManager.Builder(transport, jsonFactory, selectedCredential).build();
        ListProjectsResponse execute = resourceManager.projects().list().setPageSize(200).execute();
        return convertToGcpProjects(execute.getProjects());
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return Collections.singletonList(new GcpProject("No projects found", ""));
  }

  private List<GcpProject> convertToGcpProjects(List<Project> projects) {
    Function<Project, GcpProject> convertToGcpProject = new Function<Project, GcpProject>() {
      @Override
      public GcpProject apply(Project project) {
        return new GcpProject(project.getName(), project.getProjectId());
      }
    };
    return Lists.<Project, GcpProject>transform(projects, convertToGcpProject);
  }
}
