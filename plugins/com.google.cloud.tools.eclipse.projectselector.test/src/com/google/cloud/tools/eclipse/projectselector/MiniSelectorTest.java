/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.projectselector;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MiniSelectorTest {
  @Mock
  public IGoogleApiFactory apiFactory;
  @Rule
  public ShellTestResource shellResource = new ShellTestResource();

  @Test
  public void testNoCredential() {
    MiniSelector selector = new MiniSelector(shellResource.getShell(), apiFactory);
    assertNull(selector.getCredential());
    assertNotNull(selector.getSelection());
    assertTrue(selector.getSelection().isEmpty());
  }

  @Test
  public void testWithCredential() {
    Credential credential = mock(Credential.class);
    mockProjectsList(credential, new GcpProject("foo", "foo.id"));
    MiniSelector selector = new MiniSelector(shellResource.getShell(), apiFactory, credential);
    assertEquals(credential, selector.getCredential());
    assertNotNull(selector.getSelection());
    assertTrue(selector.getSelection().isEmpty());
    selector.setProject("foo.id");
    spinEvents(); // setProject() waits for the project list to be returned

    assertFalse(selector.getSelection().isEmpty());
    assertNotNull(selector.getProject());
    assertEquals("foo", selector.getProject().getName());
    assertEquals("foo.id", selector.getProject().getId());
  }

  @Test
  public void testListenerFired() {
    Credential credential = mock(Credential.class);
    mockProjectsList(credential, new GcpProject("foo", "foo.id"));
    final MiniSelector selector =
        new MiniSelector(shellResource.getShell(), apiFactory, credential);

    assertNotNull(selector.getSelection());
    assertTrue(selector.getSelection().isEmpty());
    final boolean[] calledAndCorrect = new boolean[] {false};
    selector.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        assertThat(event.getSelection(), instanceOf(IStructuredSelection.class));
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        assertEquals(1, selection.size());
        assertThat(selection.getFirstElement(), instanceOf(GcpProject.class));
        GcpProject gcpProject = (GcpProject) selection.getFirstElement();
        calledAndCorrect[0] =
            "foo".equals(gcpProject.getName()) && "foo.id".equals(gcpProject.getId());
      }
    });
    assertFalse(calledAndCorrect[0]);

    selector.setProject("foo.id");
    spinEvents(); // setProject() waits for the project list to be returned

    assertTrue(calledAndCorrect[0]);
  }

  private void spinEvents() {
    while (Display.getCurrent().readAndDispatch());
  }

  private void mockProjectsList(Credential credential,
      GcpProject... gcpProjects) {
    Projects projectsApi = mock(Projects.class);
    Projects.List listApi = mock(Projects.List.class);
    List<Project> projectsList = new ArrayList<>();
    for (GcpProject gcpProject : gcpProjects) {
      Project project = new Project();
      project.setName(gcpProject.getName());
      project.setProjectId(gcpProject.getId());
      projectsList.add(project);
    }
    ListProjectsResponse response = new ListProjectsResponse();
    response.setProjects(projectsList);
    try {
      doReturn(projectsApi).when(apiFactory).newProjectsApi(credential);
      doReturn(listApi).when(listApi).setPageSize(any(Integer.class));
      doReturn(listApi).when(projectsApi).list();
      doReturn(response).when(listApi).execute();
    } catch (IOException ex) {
      fail(ex.toString());
    }
  }
}
