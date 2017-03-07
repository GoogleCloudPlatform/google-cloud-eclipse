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
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.appengine.v1.model.Application;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.eclipse.googleapis.GoogleApiException;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.projectselector.model.AppEngine;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
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

  private final IGoogleApiFactory apiFactory;

  public ProjectRepository(IGoogleApiFactory apiFactory) {
    this.apiFactory = apiFactory;
  }

  /**
   * @return all active projects the account identified by {@code credential} has access to
   * @throws ProjectRepositoryException if an error happens while communicating with the backend
   */
  public List<GcpProject> getProjects(Credential credential) throws ProjectRepositoryException {
    // TODO cache results https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1374 
    try {
      if (credential != null) {
        Projects projects = apiFactory.newProjectsApi(credential);
        ListProjectsResponse execute =
            projects.list().setPageSize(PROJECT_LIST_PAGESIZE).execute();
        return convertToGcpProjects(execute.getProjects());
      } else {
        return Collections.emptyList();
      }
    } catch (IOException | GoogleApiException ex) {
      throw new ProjectRepositoryException(ex);
    }
  }

  /**
   * @return a project if the projectId identifies an existing project and the account identified by
   *     {@code credential} has access to the project
   * @throws ProjectRepositoryException if an error happens while communicating with the backend
   */
  public GcpProject getProject(Credential credential, String projectId) 
      throws ProjectRepositoryException {
    try {
      if (credential != null && !Strings.isNullOrEmpty(projectId)) {
        return convertToGcpProject(apiFactory.newProjectsApi(credential).get(projectId).execute());
      } else {
        return null;
      }
    } catch (IOException | GoogleApiException ex) {
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
   * @return true if the credential has access to the GCP project identified by {@code projectId}
   *     and the project has an App Engine application
   * @throws ProjectRepositoryException if an error other than HTTP 404 happens while retrieving the
   *     App Engine application
   */
  public AppEngine getAppEngineApplication(Credential credential, String projectId)
      throws ProjectRepositoryException {
    Preconditions.checkNotNull(credential);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(projectId));

    try {
      Application application = apiFactory.newAppsApi(credential).get(projectId).execute();

      // just in case the API changes and exception with 404 won't be
      // used to indicate a missing application
      return AppEngine.withId(application.getId());
    } catch (GoogleJsonResponseException ex) {
      if (ex.getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
        return AppEngine.NO_APPENGINE_APPLICATION;
      } else {
        String message = ex.getLocalizedMessage();
        // the message is a full json string with multiple lines, let's extract only the message
        // from the detail object if exists
        if (ex.getDetails() != null && ex.getDetails().getMessage() != null) {
          message = ex.getDetails().getMessage();
        }
        throw new ProjectRepositoryException(message, ex);
      }
    } catch (IOException | GoogleApiException ex) {
      throw new ProjectRepositoryException(ex);
    }
  }
}
