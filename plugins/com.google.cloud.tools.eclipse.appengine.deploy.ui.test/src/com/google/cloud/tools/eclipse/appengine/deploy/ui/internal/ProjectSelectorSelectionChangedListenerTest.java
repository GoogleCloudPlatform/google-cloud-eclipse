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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.deploy.ui.internal.ProjectSelectorSelectionChangedListener;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.AppEngine;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectSelectorSelectionChangedListenerTest {

  private static final String EXPECTED_LINK =
      "https://console.cloud.google.com/appengine/create?lang=java&project=projectId"
      + "&authuser=user%40example.com";
  private static final String EXPECTED_MESSAGE_WHEN_NO_APPLICATION =
      "This project does not have an App Engine application which is\n"
          + "required for deployment. <a href=\"" + EXPECTED_LINK + "\">Create an App Engine "
          + "application in the\nCloud Console</a>.";
  private static final String EXPECTED_MESSAGE_WHEN_EXCEPTION =
      "An error occurred while retrieving App Engine application:\ntestException";

  @Mock private AccountSelector accountSelector;
  @Mock private ProjectSelector projectSelector;
  @Mock private ProjectRepository projectRepository;
  @Mock private SelectionChangedEvent event;

  private ProjectSelectorSelectionChangedListener listener;

  @Before
  public void setUp() throws Exception {
    listener = new ProjectSelectorSelectionChangedListener(accountSelector, projectRepository,
                                                           projectSelector); 
  }

  @Test
  public void testSelectionChanged_emptySelection() {
    when(event.getSelection()).thenReturn(new StructuredSelection());
    listener.selectionChanged(event);
    verify(projectSelector).clearStatusLink();
  }

  @Test
  public void testSelectionChanged_repositoryException() throws ProjectRepositoryException {
    initSelectionAndAccountSelector();
    when(projectRepository.getAppEngineApplication(any(Credential.class), anyString()))
        .thenThrow(new ProjectRepositoryException("testException"));

    listener.selectionChanged(event);
    verify(projectSelector).setStatusLink(EXPECTED_MESSAGE_WHEN_EXCEPTION, null /* tooltip */);
  }

  @Test
  public void testSelectionChanged_noAppEngineApplication() throws ProjectRepositoryException {
    initSelectionAndAccountSelector();
    when(projectRepository.getAppEngineApplication(any(Credential.class), anyString()))
        .thenReturn(AppEngine.NO_APPENGINE_APPLICATION);

    listener.selectionChanged(event);
    verify(projectSelector).setStatusLink(EXPECTED_MESSAGE_WHEN_NO_APPLICATION, EXPECTED_LINK);
  }

  @Test
  public void testSelectionChanged_hasAppEngineApplication() throws ProjectRepositoryException {
    initSelectionAndAccountSelector();
    when(projectRepository.getAppEngineApplication(any(Credential.class), anyString()))
        .thenReturn(AppEngine.withId("id"));

    listener.selectionChanged(event);
    verify(projectSelector).clearStatusLink();
  }

  private void initSelectionAndAccountSelector() {
    StructuredSelection selection =
        new StructuredSelection(new GcpProject("projectName", "projectId"));
    when(event.getSelection()).thenReturn(selection);
    when(accountSelector.getSelectedCredential()).thenReturn(mock(Credential.class));
    when(accountSelector.getSelectedEmail()).thenReturn("user@example.com");
  }
}
