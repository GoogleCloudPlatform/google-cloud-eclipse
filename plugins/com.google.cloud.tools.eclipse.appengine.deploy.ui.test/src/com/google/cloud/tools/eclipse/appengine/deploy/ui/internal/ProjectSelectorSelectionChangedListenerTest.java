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

package com.google.cloud.tools.eclipse.appengine.deploy.ui.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.AppEngine;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Display;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectSelectorSelectionChangedListenerTest {

  static final String EXPECTED_LINK =
      "https://console.cloud.google.com/appengine/create?lang=java&project=projectId"
      + "&authuser=user%40example.com";
  static final String EXPECTED_MESSAGE_WHEN_NO_APPLICATION =
      "This project does not have an App Engine application which is\n"
          + "required for deployment. <a href=\"" + EXPECTED_LINK + "\">Create an App Engine "
          + "application in the\nCloud Console</a>.";
  static final String EXPECTED_MESSAGE_WHEN_EXCEPTION =
      "An error occurred while retrieving App Engine application:\ntestException";

  @Mock private AccountSelector accountSelector;
  @Mock private ProjectSelector projectSelector;
  @Mock private ProjectRepository projectRepository;
  @Mock private SelectionChangedEvent event;

  private ProjectSelectorSelectionChangedListener listener;

  @Before
  public void setUp() {
    assertNotNull(Display.getCurrent());
    when(projectSelector.getDisplay()).thenReturn(Display.getCurrent());
    when(accountSelector.getSelectedCredential()).thenReturn(mock(Credential.class));

    TableViewer viewer = mock(TableViewer.class);
    ISelection projectSelection = mock(ISelection.class);
    when(projectSelector.getViewer()).thenReturn(viewer);
    when(viewer.getSelection()).thenReturn(projectSelection);
    when(projectSelection.isEmpty()).thenReturn(false);

    listener = new ProjectSelectorSelectionChangedListener(accountSelector, projectRepository,
                                                           projectSelector);
  }

  @After
  public void tearDown() {
    if (listener.latestQueryJob != null) {
      assertEquals(Job.NONE, listener.latestQueryJob.getState());
    }
  }

  @Test
  public void testSelectionChanged_emptySelection() {
    when(event.getSelection()).thenReturn(new StructuredSelection());
    listener.selectionChanged(event);
    verify(projectSelector).clearStatusLink();
  }

  @Test
  public void testSelectionChanged_repositoryException()
      throws ProjectRepositoryException, InterruptedException {
    initSelectionAndAccountSelector();
    when(projectRepository.getAppEngineApplication(any(Credential.class), anyString()))
        .thenThrow(new ProjectRepositoryException("testException"));

    listener.selectionChanged(event);
    listener.latestQueryJob.join();
    verify(projectSelector).clearStatusLink();  // Should clear initially.
    verify(projectSelector).setStatusLink(EXPECTED_MESSAGE_WHEN_EXCEPTION, null /* tooltip */);
  }

  @Test
  public void testSelectionChanged_noAppEngineApplication()
      throws ProjectRepositoryException, InterruptedException {
    initSelectionAndAccountSelector();
    when(projectRepository.getAppEngineApplication(any(Credential.class), anyString()))
        .thenReturn(AppEngine.NO_APPENGINE_APPLICATION);

    listener.selectionChanged(event);
    listener.latestQueryJob.join();
    verify(projectSelector).clearStatusLink();  // Should clear initially.
    verify(projectSelector).setStatusLink(EXPECTED_MESSAGE_WHEN_NO_APPLICATION, EXPECTED_LINK);
  }

  @Test
  public void testSelectionChanged_hasAppEngineApplication()
      throws ProjectRepositoryException, InterruptedException {
    initSelectionAndAccountSelector();
    when(projectRepository.getAppEngineApplication(any(Credential.class), anyString()))
        .thenReturn(AppEngine.withId("id"));

    listener.selectionChanged(event);
    listener.latestQueryJob.join();
    verify(projectSelector).clearStatusLink();
  }

  @Test
  public void testSelectionChanged_doNotRunQueryJobIfCached() throws ProjectRepositoryException {
    GcpProject gcpProject = new GcpProject("projectName", "projectId");
    initSelectionAndAccountSelector(gcpProject);
    gcpProject.setAppEngine(AppEngine.withId("id"));

    listener.selectionChanged(event);
    assertNull(listener.latestQueryJob);
    verify(projectRepository, never()).getAppEngineApplication(any(Credential.class), anyString());
    verify(projectSelector).clearStatusLink();
  }

  @Test
  public void testSelectionChanged_whenCachedResultIsNoAppEngineApplication()
      throws ProjectRepositoryException {
    GcpProject gcpProject = new GcpProject("projectName", "projectId");
    initSelectionAndAccountSelector(gcpProject);
    gcpProject.setAppEngine(AppEngine.NO_APPENGINE_APPLICATION);

    listener.selectionChanged(event);
    assertNull(listener.latestQueryJob);
    verify(projectRepository, never()).getAppEngineApplication(any(Credential.class), anyString());
    verify(projectSelector).setStatusLink(EXPECTED_MESSAGE_WHEN_NO_APPLICATION, EXPECTED_LINK);
  }

  @Test
  public void testSelectionChanged_changeSelectedProject()
      throws ProjectRepositoryException, InterruptedException {
    when(projectRepository.getAppEngineApplication(any(Credential.class), eq("oldProjectId")))
        .thenThrow(new ProjectRepositoryException("testException"));
    when(projectRepository.getAppEngineApplication(any(Credential.class), eq("projectId")))
        .thenReturn(AppEngine.NO_APPENGINE_APPLICATION);

    initSelectionAndAccountSelector(new GcpProject("oldProjectName", "oldProjectId"));
    listener.selectionChanged(event);

    Job oldJob = listener.latestQueryJob;
    assertNotNull(oldJob);
    oldJob.join();

    initSelectionAndAccountSelector();
    listener.selectionChanged(event);

    Job newJob = listener.latestQueryJob;
    assertNotNull(newJob);
    assertNotEquals(oldJob, newJob);
    newJob.join();

    verify(projectRepository).getAppEngineApplication(any(Credential.class), eq("oldProjectId"));
    verify(projectRepository).getAppEngineApplication(any(Credential.class), eq("projectId"));
    verify(projectSelector).setStatusLink(EXPECTED_MESSAGE_WHEN_NO_APPLICATION, EXPECTED_LINK);
  }

  private void initSelectionAndAccountSelector() {
    initSelectionAndAccountSelector(new GcpProject("projectName", "projectId"));
  }

  private void initSelectionAndAccountSelector(GcpProject gcpProject) {
    StructuredSelection selection = new StructuredSelection(gcpProject);
    when(event.getSelection()).thenReturn(selection);
    when(accountSelector.getSelectedEmail()).thenReturn("user@example.com");
  }
}
