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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper for the GCP Cloud Resource Manager API.
 */
public class ProjectRepository {

  private static final int PROJECT_LIST_PAGESIZE = 300;
  private static final String PROJECT_DELETE_REQUESTED = "DELETE_REQUESTED";

  /**
   * @return all active projects the account identified by {@code credential} has access to
   * @throws ProjectRepositoryException if an error happens while communicating with the backend
   */
  public List<GcpProject> getProjects(Credential credential) throws ProjectRepositoryException {
    // TODO cache results https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1374 
    try {
      if (credential != null) {
        Projects projects = getProjectsApi(credential);
        ListProjectsResponse execute =
            projects.list().setPageSize(PROJECT_LIST_PAGESIZE).execute();
        return convertToGcpProjects(execute.getProjects());
      } else {
        return Collections.emptyList();
      }
    } catch (IOException ex) {
      throw new ProjectRepositoryException(ex);
    }
  }

  /**
   * @return a project if the projectId identifies an existing project and the account identified by
   * {@code credential} has access to the project
   * @throws ProjectRepositoryException if an error happens while communicating with the backend
   */
  public GcpProject getProject(Credential credential,
                               String projectId) throws ProjectRepositoryException {
    try {
      if (credential != null && !Strings.isNullOrEmpty(projectId)) {
        return convertToGcpProject(getProjectsApi(credential).get(projectId).execute());
      } else {
        return null;
      }
    } catch (IOException ex) {
      throw new ProjectRepositoryException(ex);
    }
  }

  private Projects getProjectsApi(Credential credential) {
    JsonFactory jsonFactory = new JacksonFactory();
    HttpTransport transport = new NetHttpTransport();
    CloudResourceManager resourceManager =
        new CloudResourceManager.Builder(transport, jsonFactory, credential)
            .setApplicationName(CloudToolsInfo.USER_AGENT).build();
    Projects projects = resourceManager.projects();
    return projects;
  }

  private List<GcpProject> convertToGcpProjects(List<Project> projects) {
    List<GcpProject> gcpProjects = new ArrayList<>();
    for (Project project : projects) {
      if (!PROJECT_DELETE_REQUESTED.equals(project.getLifecycleState())) {
        gcpProjects.add(convertToGcpProject(project));
      }
    }
    return gcpProjects;
  }

  private GcpProject convertToGcpProject(Project project) {
    return new GcpProject(project.getName(), project.getProjectId());
  }

}
