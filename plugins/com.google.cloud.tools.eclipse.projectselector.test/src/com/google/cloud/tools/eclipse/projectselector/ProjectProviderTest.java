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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.util.jobs.Consumer;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import java.util.List;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests ProjectProvider, an IStructuredContentProvider.
 */
public class ProjectProviderTest {
  private ProjectRepository repo;
  private ProjectsProvider provider;
  private Viewer viewer;

  @Before
  public void setUp() {
    repo = mock(ProjectRepository.class);
    provider = new ProjectsProvider(repo);

    viewer = mock(Viewer.class);
    Control control = mock(Control.class);
    doReturn(false).when(control).isDisposed();
    doReturn(Display.getCurrent()).when(control).getDisplay();
    doReturn(control).when(viewer).getControl();
  }

  @Test
  public void testInputChanged_null_noJob() {
    provider.inputChanged(viewer, null, null);
    assertNull(provider.fetchProjectsJob);
  }

  @Test
  public void testInputChanged_credential_launchesJob()
      throws InterruptedException, ProjectRepositoryException {
    Credential credential = mock(Credential.class);
    List<GcpProject> projects = Collections.singletonList(new GcpProject("name", "id"));
    doReturn(projects).when(repo).getProjects(credential);

    provider.inputChanged(viewer, null, credential);
    assertNotNull(provider.fetchProjectsJob);
    provider.fetchProjectsJob.join();
    assertTrue(provider.fetchProjectsJob.getComputation().isPresent());
    GcpProject[] returned = (GcpProject[]) provider.fetchProjectsJob.getComputation().get();
    assertEquals(projects.size(), returned.length);
    assertEquals(projects.get(0), returned[0]);
  }

  @Test
  public void testGetElements_nullCredential() {
    assertNull(provider.fetchProjectsJob);
    Object[] result = provider.getElements(null);
    assertEquals(0, result.length);
  }

  @Test
  public void testGetElements_nullCredential_otherCredential() {
    assertNull(provider.fetchProjectsJob);
    Object[] result = provider.getElements(mock(Credential.class));
    assertEquals(0, result.length);
  }

  @Test
  public void testGetElements_otherCredential() throws ProjectRepositoryException {
    Credential credential = mock(Credential.class);
    List<GcpProject> projects = Collections.singletonList(new GcpProject("name", "id"));
    doReturn(projects).when(repo).getProjects(credential);

    provider.inputChanged(viewer, null, credential);
    assertNotNull(provider.fetchProjectsJob);
    Object[] result = provider.getElements(mock(Credential.class));
    assertEquals(0, result.length);
  }

  @Test
  public void testGetElements_sameCredential()
      throws InterruptedException, ProjectRepositoryException {
    Credential credential = mock(Credential.class);
    List<GcpProject> projects = Collections.singletonList(new GcpProject("name", "id"));
    doReturn(projects).when(repo).getProjects(credential);

    provider.inputChanged(viewer, null, credential);
    assertNotNull(provider.fetchProjectsJob);
    provider.fetchProjectsJob.join();
    Object[] result = provider.getElements(credential);
    assertEquals(projects.size(), result.length);
    assertEquals(projects.get(0), result[0]);
  }


  @Test
  public void testResolve_nullCredential() {
    @SuppressWarnings("unchecked")
    Consumer<GcpProject> callback = mock(Consumer.class);
    assertNull(provider.fetchProjectsJob);
    provider.resolve("foo", MoreExecutors.directExecutor(), callback);
    verify(callback, never()).accept(any(GcpProject.class));
  }

  @Test
  public void testResolve_withCredential() throws ProjectRepositoryException, InterruptedException {
    Credential credential = mock(Credential.class);
    List<GcpProject> projects = Collections.singletonList(new GcpProject("name", "id"));
    doReturn(projects).when(repo).getProjects(credential);
    provider.inputChanged(viewer, null, credential);

    assertNotNull(provider.fetchProjectsJob);
    provider.fetchProjectsJob.join();

    @SuppressWarnings("unchecked")
    Consumer<GcpProject> callback = mock(Consumer.class);
    provider.resolve("id", MoreExecutors.directExecutor(), callback);
    verify(callback).accept(projects.get(0));
  }

}
