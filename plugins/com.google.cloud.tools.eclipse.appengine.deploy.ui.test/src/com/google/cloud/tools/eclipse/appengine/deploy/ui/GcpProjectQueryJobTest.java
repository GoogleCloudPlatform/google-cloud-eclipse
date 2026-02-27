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

package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GcpProjectQueryJobTest {

  @Mock private Credential credential;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectSelector projectSelector;
  @Mock private Predicate<Job> isLatestQueryJob;

  private final DataBindingContext dataBindingContext = new DataBindingContext();
  private final List<GcpProject> projects = new ArrayList<>();

  private Job queryJob;

  @Before
  public void setUp() throws ProjectRepositoryException {
    assertNotNull(Display.getCurrent());
    when(projectSelector.getDisplay()).thenReturn(Display.getCurrent());

    queryJob = new GcpProjectQueryJob(projectRepository, projectSelector,
        dataBindingContext, isLatestQueryJob);

    when(projectSelector.isDisposed()).thenReturn(false);
    when(projectRepository.getProjects()).thenReturn(projects);
    when(isLatestQueryJob.test(queryJob)).thenReturn(true);
  }

  @After
  public void tearDown() {
    assertEquals(Job.NONE, queryJob.getState());
  }

  @Test
  public void testRun_setsProjects() throws InterruptedException, ProjectRepositoryException {
    queryJob.schedule();
    queryJob.join();

    verify(projectRepository).getProjects();
    verify(isLatestQueryJob).test(queryJob);
    verify(projectSelector).isDisposed();
    verify(projectSelector).setProjects(projects);
  }

  @Test
  public void testRun_abandonIfDisposed() throws InterruptedException, ProjectRepositoryException {
    when(projectSelector.isDisposed()).thenReturn(true);

    queryJob.schedule();
    queryJob.join();

    verify(projectRepository).getProjects();
    verify(projectSelector, never()).setProjects(projects);
  }

  @Test
  public void testRun_abandonIfNotLatestJob()
      throws InterruptedException, ProjectRepositoryException {
    when(isLatestQueryJob.test(queryJob)).thenReturn(false);

    queryJob.schedule();
    queryJob.join();

    verify(projectRepository).getProjects();
    verify(projectSelector, never()).setProjects(projects);
  }

  @Test
  public void testRun_abandonStaleJob() throws InterruptedException, ProjectRepositoryException {
    List<GcpProject> anotherProjectList = new ArrayList<>();
    anotherProjectList.add(null); // so not equals to projects
    
    ProjectRepository projectRepository2 = mock(ProjectRepository.class);
    when(projectRepository2.getProjects()).thenReturn(anotherProjectList);

    // This second job is stale, i.e., it was fired, but user has selected another credential.
    Predicate<Job> notLatest = job -> false;
    Job staleJob = new GcpProjectQueryJob(projectRepository2,
        projectSelector, dataBindingContext, notLatest);

    queryJob.schedule();
    queryJob.join();
    // Make the stale job complete even after "queryJob" finishes.
    staleJob.schedule();
    staleJob.join();

    verify(projectRepository).getProjects();
    verify(projectRepository2).getProjects();

    verify(projectSelector).setProjects(projects);
    verify(projectSelector, never()).setProjects(anotherProjectList);
  }
}
