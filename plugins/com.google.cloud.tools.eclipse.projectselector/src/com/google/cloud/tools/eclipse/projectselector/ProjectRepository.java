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

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.appengine.v1.model.Application;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.projectselector.model.AppEngine;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for the GCP Cloud Resource Manager API.
 */
public class ProjectRepository {

  // API may return fewer than 700 results in one request, but 600 and change is the
  // most we've seen in any one account to date.
  private static final int PROJECT_LIST_PAGESIZE = 700;
  
  private static final String PROJECT_DELETE_REQUESTED = "DELETE_REQUESTED";


  public ProjectRepository() {
  }

  /**
   * @return all active projects the account identified by {@code credential} has access to
   * @throws ProjectRepositoryException if an error happens while communicating with the backend
   */
  public List<GcpProject> getProjects() throws ProjectRepositoryException {
    Preconditions.checkState(GoogleApiFactory.INSTANCE.getCredential().isPresent());
    // TODO cache results https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1374
    try {
      Projects projects = GoogleApiFactory.INSTANCE.newProjectsApi();
      
      String token = null;
      List<Project> projectList = new ArrayList<>();
      do {
        Projects.List listRequest = projects.list().setPageSize(PROJECT_LIST_PAGESIZE);
        if (token != null) {
          listRequest = listRequest.setPageToken(token); 
        }
        ListProjectsResponse response = listRequest.execute();
        List<Project> responseProjects = response.getProjects();
        if (responseProjects != null) {
          projectList.addAll(responseProjects);
        }
        token = response.getNextPageToken();
      } while (token != null);
      List<GcpProject> gcpProjects = convertToGcpProjects(projectList);
      return gcpProjects;
    } catch (IOException ex) {
      throw new ProjectRepositoryException(ex);
    }
  }

  /**
   * @return a project if the projectId identifies an existing project and the account identified by
   *     {@code credential} has access to the project
   * @throws ProjectRepositoryException if an error happens while communicating with the backend
   */
  public GcpProject getProject(String projectId)
      throws ProjectRepositoryException {
    try {
      if (GoogleApiFactory.INSTANCE.getCredential().isPresent() && !Strings.isNullOrEmpty(projectId)) {
        return convertToGcpProject(GoogleApiFactory.INSTANCE.newProjectsApi().get(projectId).execute());
      } else {
        return null;
      }
    } catch (IOException ex) {
      throw new ProjectRepositoryException(ex);
    }
  }

  @VisibleForTesting
  static List<GcpProject> convertToGcpProjects(List<Project> projects) {
    List<GcpProject> gcpProjects = new ArrayList<>();
    if (projects != null) {
      for (Project project : projects) {
        if (!PROJECT_DELETE_REQUESTED.equals(project.getLifecycleState())) {
          gcpProjects.add(convertToGcpProject(project));
        }
      }
    }
    return gcpProjects;
  }

  private static GcpProject convertToGcpProject(Project project) {
    Preconditions.checkNotNull(project);
    return new GcpProject(project.getName(), project.getProjectId());
  }

  /**
   * @return {@link AppEngine} if the credential has access to the GCP project identified by
   *     {@code projectId} and the project has an App Engine application
   * @throws ProjectRepositoryException if an error other than HTTP 404 happens while retrieving the
   *     App Engine application
   */
  public AppEngine getAppEngineApplication(String projectId)
      throws ProjectRepositoryException {
    Preconditions.checkState(GoogleApiFactory.INSTANCE.getCredential().isPresent());
    Preconditions.checkArgument(!Strings.isNullOrEmpty(projectId));

    try {
      Application application = GoogleApiFactory.INSTANCE.newAppsApi().get(projectId).execute();

      // just in case the API changes and exception with 404 won't be
      // used to indicate a missing application
      return AppEngine.withId(application.getId());
    } catch (GoogleJsonResponseException ex) {
      if (ex.getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
        return AppEngine.NO_APPENGINE_APPLICATION;
      } else if (ex.getStatusCode() == HttpStatusCodes.STATUS_CODE_FORBIDDEN) {
        String message = ex.getLocalizedMessage();
        // the message is a full json string with multiple lines, let's extract only the message
        // from the detail object if exists
        if (ex.getDetails() != null && ex.getDetails().getMessage() != null) {
          message = ex.getDetails().getMessage();
        }
        throw new ApplicationPermissionsException(message, ex);
      } else {
        String message = ex.getLocalizedMessage();
        // the message is a full json string with multiple lines, let's extract only the message
        // from the detail object if exists
        if (ex.getDetails() != null && ex.getDetails().getMessage() != null) {
          message = ex.getDetails().getMessage();
        }
        throw new ProjectRepositoryException(message, ex);
      }
    } catch (IOException ex) {
      throw new ProjectRepositoryException(ex);
    }
  }
}
