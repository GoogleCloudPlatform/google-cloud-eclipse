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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.login.ui.AccountSelector;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.util.Arrays;
import java.util.List;
import org.eclipse.swt.widgets.TableColumn;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectSelectorTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();
  @Mock private AccountSelector accountSelector;
  @Mock private ProjectRepository projectRepository;
  @Captor ArgumentCaptor<Runnable> accountSelectedHandler;

  @Test
  public void testIfAccountSelectorChangeThenProjectsAreQueried() throws ProjectRepositoryException {
    doNothing().when(accountSelector).addSelectionListener(accountSelectedHandler.capture());
    new ProjectSelector(shellResource.getShell(), accountSelector, projectRepository);
    accountSelectedHandler.getValue().run();
    verify(projectRepository).getProjects(any(Credential.class));
  }

  @Test
  public void testIfProjectQueryFailsThenEmptyTable() throws ProjectRepositoryException {
    doNothing().when(accountSelector).addSelectionListener(accountSelectedHandler.capture());
    when(projectRepository.getProjects(any(Credential.class))).thenThrow(new ProjectRepositoryException("test"));
    ProjectSelector projectSelector =
        new ProjectSelector(shellResource.getShell(), accountSelector, projectRepository);
    accountSelectedHandler.getValue().run();
    assertThat(projectSelector.getViewer().getTable().getItemCount(), is(0));
  }

  @Test
  public void testCreatedColumns() {
    ProjectSelector projectSelector =
        new ProjectSelector(shellResource.getShell(), accountSelector, projectRepository);
    TableColumn[] columns = projectSelector.getViewer().getTable().getColumns();
    assertThat(columns.length, is(2));
    assertThat(columns[0].getText(), is("Name"));
    assertThat(columns[1].getText(), is("ID"));
  }

  @Test
  public void testProjectsAreSortedAlphabetically() throws Exception {
    doNothing().when(accountSelector).addSelectionListener(accountSelectedHandler.capture());
    when(projectRepository.getProjects(any(Credential.class))).thenReturn(getUnsortedProjectList());

    ProjectSelector projectSelector =
        new ProjectSelector(shellResource.getShell(), accountSelector, projectRepository);
    accountSelectedHandler.getValue().run();

    assertThat(((GcpProject) projectSelector.getViewer().getElementAt(0)).getName(), is("a"));
    assertThat(((GcpProject) projectSelector.getViewer().getElementAt(1)).getName(), is("b"));
    assertThat(((GcpProject) projectSelector.getViewer().getElementAt(2)).getName(), is("c"));
    assertThat(((GcpProject) projectSelector.getViewer().getElementAt(3)).getName(), is("d"));
  }

  private List<GcpProject> getUnsortedProjectList() {
    return Arrays.asList(new GcpProject("b", "b"),
                         new GcpProject("a", "a"),
                         new GcpProject("d", "d"),
                         new GcpProject("c", "c"));
  }
}
