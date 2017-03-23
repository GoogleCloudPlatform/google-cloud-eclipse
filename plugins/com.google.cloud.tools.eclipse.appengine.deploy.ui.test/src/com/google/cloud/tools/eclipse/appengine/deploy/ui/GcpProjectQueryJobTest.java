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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepository;
import com.google.cloud.tools.eclipse.projectselector.ProjectRepositoryException;
import com.google.cloud.tools.eclipse.projectselector.ProjectSelector;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GcpProjectQueryJobTest {

  @Rule public ShellTestResource shell = new ShellTestResource();

  @Mock private Credential credential;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectSelector projectSelector;
  @Mock private Predicate<Job> isLatestQueryJob;

  private List<GcpProject> projects = new ArrayList<>(Arrays.asList(
      mock(GcpProject.class), mock(GcpProject.class)));

  private Job queryJob;

  @Before
  public void setUp() throws ProjectRepositoryException {
    queryJob = new GcpProjectQueryJob(
        credential, projectRepository, projectSelector, isLatestQueryJob);

    when(projectRepository.getProjects(credential)).thenReturn(projects);
    when(isLatestQueryJob.apply(queryJob)).thenReturn(true);
    when(projectSelector.getShell()).thenReturn(shell.getShell());
    when(projectSelector.getDisplay()).thenReturn(shell.getDisplay());
  }

  @After
  public void tearDown() {
    assertEquals(Job.NONE, queryJob.getState());
  }

  @Test(expected = NullPointerException.class)
  public void testNullCredential() {
    new GcpProjectQueryJob(null, projectRepository, projectSelector, isLatestQueryJob);
  }

  @Test
  public void testRun_setsProjects() throws InterruptedException, ProjectRepositoryException {
    queryJob.schedule();
    queryJob.join();

    verify(projectRepository).getProjects(credential);
    verify(projectSelector).setProjects(projects);
  }

  @Test
  public void testRun_abandonIfDisposed() throws InterruptedException, ProjectRepositoryException {
    when(projectSelector.isDisposed()).thenReturn(true);

    queryJob.schedule();
    queryJob.join();

    verify(projectRepository).getProjects(credential);
    verify(projectSelector).setProjects(projects);
  }
}
