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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.testing.json.GoogleJsonResponseExceptionFactoryTesting;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.appengine.v1.Appengine.Apps;
import com.google.api.services.appengine.v1.model.Application;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects.Get;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.eclipse.googleapis.GoogleApiFactory;
import com.google.cloud.tools.eclipse.projectselector.model.AppEngine;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectRepositoryTest {

  @Mock private GoogleApiFactory apiFactory;
  private ProjectRepository repository;
  private Project project;

  @Before
  public void setUp() {
    repository = new ProjectRepository(apiFactory);
    project = new Project();
    project.setName("projectName").setProjectId("projectId");
  }

  @Test
  public void testGetProjects_nullCredential() throws ProjectRepositoryException {
    Credential credential = null;
    List<GcpProject> projects = repository.getProjects(credential);
    Assert.assertTrue(projects.isEmpty());
  }

  @Test(expected = ProjectRepositoryException.class)
  public void testGetProjects_exceptionInRequest() throws IOException, ProjectRepositoryException {
    com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects.List list =
        initializeListRequest();
    when(list.execute()).thenThrow(new IOException("test exception"));

    repository.getProjects(mock(Credential.class));
  }

  @Test
  public void testGetProjects_successful() throws IOException, ProjectRepositoryException {
    com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects.List list =
        initializeListRequest();
    ListProjectsResponse response = new ListProjectsResponse();
    response.setProjects(Collections.singletonList(project));
    when(list.execute()).thenReturn(response);

    List<GcpProject> gcpProjects = repository.getProjects(mock(Credential.class));

    assertNotNull(gcpProjects);
    assertThat(gcpProjects.size(), is(1));
    GcpProject gcpProject = gcpProjects.get(0);
    assertThat(gcpProject.getName(), is("projectName"));
    assertThat(gcpProject.getId(), is("projectId"));
  }

  @Test
  public void testGetProjects_onlyDeletedProjectsReturned() throws IOException, ProjectRepositoryException {
    com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects.List list =
        initializeListRequest();
    ListProjectsResponse response = new ListProjectsResponse();
    response.setProjects(Collections.singletonList(project));
    project.setLifecycleState("DELETE_REQUESTED");
    when(list.execute()).thenReturn(response);

    List<GcpProject> gcpProjects = repository.getProjects(mock(Credential.class));

    assertNotNull(gcpProjects);
    assertTrue(gcpProjects.isEmpty());
  }

  @Test
  public void testGetProject_nullCredential() throws ProjectRepositoryException {
    assertNull(repository.getProject(null /*credential */, "projectId"));
  }

  @Test
  public void testGetProject_nullProjectId() throws ProjectRepositoryException {
    assertNull(repository.getProject(mock(Credential.class), null /* projectId */));
  }

  @Test
  public void testGetProject_emptyProjectId() throws ProjectRepositoryException {
    assertNull(repository.getProject(mock(Credential.class), ""));
  }

  @Test(expected = ProjectRepositoryException.class)
  public void testGetProject_exceptionInRequest() throws IOException, ProjectRepositoryException {
    Projects projects = mock(Projects.class);
    when(apiFactory.newProjectsApi(any(Credential.class))).thenReturn(projects);
    Get get = mock(Get.class);
    when(projects.get(anyString())).thenReturn(get);
    when(get.execute()).thenThrow(new IOException("test exception"));

    repository.getProject(mock(Credential.class), "projectId");
  }

  @Test
  public void testGetProject_successful() throws IOException, ProjectRepositoryException {
    Projects projects = mock(Projects.class);
    when(apiFactory.newProjectsApi(any(Credential.class))).thenReturn(projects);
    Get get = mock(Get.class);
    when(projects.get(anyString())).thenReturn(get);
    when(get.execute()).thenReturn(project);

    GcpProject gcpProject = repository.getProject(mock(Credential.class), "projectId");

    assertNotNull(gcpProject);
    assertThat(gcpProject.getName(), is("projectName"));
    assertThat(gcpProject.getId(), is("projectId"));
  }

  @Test(expected = NullPointerException.class)
  public void testHasAppEngineApplication_nullCredential() throws ProjectRepositoryException {
    repository.getAppEngineApplication(null, "projectId");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHasAppEngineApplication_nullProjectId() throws ProjectRepositoryException {
    repository.getAppEngineApplication(mock(Credential.class), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHasAppEngineApplication_emptyProjectId() throws ProjectRepositoryException {
    repository.getAppEngineApplication(mock(Credential.class), "");
  }

  @Test
  public void testHasAppengineApplication_hasApplication() throws IOException, ProjectRepositoryException {
    com.google.api.services.appengine.v1.Appengine.Apps.Get get = initializeGetRequest();
    Application application = new Application();
    application.setId("id");
    when(get.execute()).thenReturn(application);

    assertThat(repository.getAppEngineApplication(mock(Credential.class), "projectId"),
        is(not(AppEngine.NO_APPENGINE_APPLICATION)));
  }

  @Test
  public void testHasAppengineApplication_noApplication() throws IOException, ProjectRepositoryException {
    com.google.api.services.appengine.v1.Appengine.Apps.Get get = initializeGetRequest();
    GoogleJsonResponseException notFoundException =
        GoogleJsonResponseExceptionFactoryTesting.newMock(new JacksonFactory(), 404, "Not found");
    when(get.execute()).thenThrow(notFoundException);

    assertThat(repository.getAppEngineApplication(mock(Credential.class), "projectId"),
        is(AppEngine.NO_APPENGINE_APPLICATION));
  }

  @Test(expected = ProjectRepositoryException.class)
  public void testHasAppengineApplication_exception() throws IOException, ProjectRepositoryException {
    com.google.api.services.appengine.v1.Appengine.Apps.Get get = initializeGetRequest();
    when(get.execute()).thenThrow(new IOException("test exception"));

    repository.getAppEngineApplication(mock(Credential.class), "projectId");
  }

  @Test(expected = ProjectRepositoryException.class)
  public void testHasAppengineApplication_GoogleJsonResponseException()
      throws IOException, ProjectRepositoryException {
    com.google.api.services.appengine.v1.Appengine.Apps.Get get = initializeGetRequest();
    GoogleJsonResponseException exception =
        GoogleJsonResponseExceptionFactoryTesting.newMock(new JacksonFactory(), 500, "Server Error");
    when(get.execute()).thenThrow(exception);

    repository.getAppEngineApplication(mock(Credential.class), "projectId");
  }

  @Test
  public void testConvertToGcpProjects_null() {
    List<GcpProject> projects = ProjectRepository.convertToGcpProjects(null);
    Assert.assertTrue(projects.isEmpty());
  }

  private com.google.api.services.appengine.v1.Appengine.Apps.Get
  initializeGetRequest() throws IOException {
    Apps apps = mock(Apps.class);
    when(apiFactory.newAppsApi(any(Credential.class))).thenReturn(apps);
    
    com.google.api.services.appengine.v1.Appengine.Apps.Get get =
        mock(com.google.api.services.appengine.v1.Appengine.Apps.Get.class);
    when(apps.get(anyString())).thenReturn(get);
    return get;
  }
  
  private com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects.List
  initializeListRequest() throws IOException {
    Projects projects = mock(Projects.class);
    when(apiFactory.newProjectsApi(any(Credential.class))).thenReturn(projects);
    com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects.List list =
        mock(com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects.List.class);
    when(projects.list()).thenReturn(list);
    when(list.setPageSize(anyInt())).thenReturn(list);
    return list;
  }

}
