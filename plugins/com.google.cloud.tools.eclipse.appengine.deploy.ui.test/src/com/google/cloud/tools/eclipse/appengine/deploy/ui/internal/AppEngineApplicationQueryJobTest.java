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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.AppEngine;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.common.base.Predicate;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AppEngineApplicationQueryJobTest {

  private static final String EXPECTED_LINK =
      ProjectSelectorSelectionChangedListenerTest.EXPECTED_LINK;
  private static final String EXPECTED_MESSAGE_WHEN_NO_APPLICATION =
      ProjectSelectorSelectionChangedListenerTest.EXPECTED_MESSAGE_WHEN_NO_APPLICATION;
  private static final String EXPECTED_MESSAGE_WHEN_EXCEPTION =
      ProjectSelectorSelectionChangedListenerTest.EXPECTED_MESSAGE_WHEN_EXCEPTION;

  private GcpProject project = new GcpProject("name", "projectId");
  @Mock private Credential credential;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectSelector projectSelector;
  @Mock private Predicate<Job> isLatestQueryJob;

  private Job queryJob;

  @Before
  public void setUp() throws ProjectRepositoryException {
    assertNotNull(Display.getCurrent());

    queryJob = new AppEngineApplicationQueryJob(project, credential, projectRepository,
        projectSelector, EXPECTED_LINK, isLatestQueryJob, Display.getCurrent());

    when(projectSelector.isDisposed()).thenReturn(false);
    when(isLatestQueryJob.apply(queryJob)).thenReturn(true);
  }

  @After
  public void tearDown() {
    assertEquals(Job.NONE, queryJob.getState());
  }

  @Test
  public void testRun_projectHasNoApplication()
      throws ProjectRepositoryException, InterruptedException {
    when(projectRepository.getAppEngineApplication(credential, "projectId"))
        .thenReturn(AppEngine.NO_APPENGINE_APPLICATION);
    assertNull(project.getAppEngine());

    queryJob.schedule();
    queryJob.join();

    verify(projectRepository).getAppEngineApplication(credential, "projectId");
    verify(isLatestQueryJob).apply(queryJob);
    verify(projectSelector).isDisposed();
    verify(projectSelector).setStatusLink(EXPECTED_MESSAGE_WHEN_NO_APPLICATION, EXPECTED_LINK);

    assertEquals(AppEngine.NO_APPENGINE_APPLICATION, project.getAppEngine());
  }

  @Test
  public void testRun_projectHasApplication()
      throws ProjectRepositoryException, InterruptedException {
    AppEngine appEngine = AppEngine.withId("unique-id");
    when(projectRepository.getAppEngineApplication(credential, "projectId")).thenReturn(appEngine);

    queryJob.schedule();
    queryJob.join();

    verify(isLatestQueryJob, never()).apply(queryJob);
    verify(projectSelector, never()).isDisposed();
    verify(projectSelector, never()).setStatusLink(anyString(), anyString());

    assertTrue(appEngine == project.getAppEngine());
  }

  @Test
  public void testRun_queryError() throws ProjectRepositoryException, InterruptedException {
    when(projectRepository.getAppEngineApplication(credential, "projectId"))
        .thenThrow(new ProjectRepositoryException("testException"));

    queryJob.schedule();
    queryJob.join();

    verify(isLatestQueryJob).apply(queryJob);
    verify(projectSelector).isDisposed();
    verify(projectSelector).setStatusLink(EXPECTED_MESSAGE_WHEN_EXCEPTION, null);

    assertNull(project.getAppEngine());
  }

  @Test
  public void testRun_abandonIfDisposed() throws InterruptedException, ProjectRepositoryException {
    when(projectSelector.isDisposed()).thenReturn(true);
    when(projectRepository.getAppEngineApplication(credential, "projectId"))
        .thenReturn(AppEngine.NO_APPENGINE_APPLICATION);

    queryJob.schedule();
    queryJob.join();

    verify(projectSelector).isDisposed();
    verify(projectSelector, never()).setStatusLink(anyString(), anyString());
  }

  @Test
  public void testRun_abandonIfNotLatestJob()
      throws InterruptedException, ProjectRepositoryException {
    when(isLatestQueryJob.apply(queryJob)).thenReturn(false);
    when(projectRepository.getAppEngineApplication(credential, "projectId"))
        .thenReturn(AppEngine.NO_APPENGINE_APPLICATION);

    queryJob.schedule();
    queryJob.join();

    verify(isLatestQueryJob).apply(queryJob);
    verify(projectSelector, never()).setStatusLink(anyString(), anyString());
  }
}
