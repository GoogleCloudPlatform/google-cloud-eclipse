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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.projectselector.model.GcpProject;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.TableColumn;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectSelectorTest {

  @Rule public ShellTestResource shellResource = new ShellTestResource();

  private ProjectSelector projectSelector;

  @Before
  public void setUp() {
    projectSelector = new ProjectSelector(shellResource.getShell());
  }

  @Test
  public void testCreatedColumns() {
    TableColumn[] columns = projectSelector.getViewer().getTable().getColumns();
    assertThat(columns.length, is(2));
    assertThat(columns[0].getText(), is("Name"));
    assertThat(columns[1].getText(), is("ID"));
  }

  @Test
  public void testProjectsAreSortedAlphabetically() {
    projectSelector.setProjects(getUnsortedProjectList());

    assertThat(getVisibleProjectAtIndex(0).getName(), is("a"));
    assertThat(getVisibleProjectAtIndex(1).getName(), is("b"));
    assertThat(getVisibleProjectAtIndex(2).getName(), is("c"));
    assertThat(getVisibleProjectAtIndex(3).getName(), is("d"));
  }

  private GcpProject getVisibleProjectAtIndex(int index) {
    return (GcpProject) projectSelector.getViewer().getTable().getItem(index).getData();
  }

  @Test
  public void testSetProjectMaintainsSelection() {
    List<GcpProject> projects = getUnsortedProjectList();
    GcpProject selectedProject = projects.get(3);

    projectSelector.setProjects(projects);
    projectSelector.getViewer().setSelection(new StructuredSelection(selectedProject));
    projectSelector.setProjects(projects.subList(2, projects.size()));

    IStructuredSelection selection = projectSelector.getViewer().getStructuredSelection();
    assertThat(selection.size(), is(1));
    assertThat((GcpProject) selection.getFirstElement(), is(selectedProject));
  }

  @Test
  public void testMatches() {
    IValueProperty property = mock(IValueProperty.class);
    when(property.getValue(any())).thenReturn("a");
    assertTrue(ProjectSelector.matches(new String[] { "a" }, new Object(), new IValueProperty[] { property }));
    assertFalse(
        ProjectSelector.matches(new String[] {"b"}, new Object(), new IValueProperty[] {property}));
  }

  @Test
  public void testGetSelectedProjectId_nothingSelected() {
    projectSelector.setProjects(getUnsortedProjectList());
    assertEquals("", projectSelector.getSelectedProjectId());
  }

  @Test
  public void testGetSelectedProjectId() {
    projectSelector.setProjects(getUnsortedProjectList());
    projectSelector.setSelection(new StructuredSelection(new GcpProject("d", "d")));
    assertEquals("d", projectSelector.getSelectedProjectId());
  }

  @Test
  public void testSelectProjectId() {
    projectSelector.setProjects(getUnsortedProjectList());
    assertTrue(projectSelector.selectProjectId("b"));
    assertEquals("b", projectSelector.getSelectedProjectId());

    assertTrue(projectSelector.selectProjectId("d"));
    assertEquals("d", projectSelector.getSelectedProjectId());
  }

  @Test
  public void testSelectProjectId_deselect() {
    projectSelector.setProjects(getUnsortedProjectList());
    assertTrue(projectSelector.selectProjectId("b"));
    assertEquals("b", projectSelector.getSelectedProjectId());

    assertFalse(projectSelector.selectProjectId("non-existing-id"));
    assertEquals("", projectSelector.getSelectedProjectId());
  }

  @Test
  public void testIsProjectIdAvailable() {
    projectSelector.setProjects(getUnsortedProjectList());
    assertTrue(projectSelector.isProjectIdAvailable("d"));
  }

  @Test
  public void testIsProjectIdAvailable_notAvailable() {
    projectSelector.setProjects(getUnsortedProjectList());
    assertFalse(projectSelector.isProjectIdAvailable("non-existing-project-id"));
  }

  @Test
  public void testIsProjectIdAvailable_null() {
    projectSelector.setProjects(getUnsortedProjectList());
    assertFalse(projectSelector.isProjectIdAvailable(null));
  }

  @Test
  public void testIsProjectIdAvailable_emptyString() {
    projectSelector.setProjects(getUnsortedProjectList());
    assertFalse(projectSelector.isProjectIdAvailable(""));
  }

  private static List<GcpProject> getUnsortedProjectList() {
    return Arrays.asList(new GcpProject("b", "b"),
                         new GcpProject("a", "a"),
                         new GcpProject("d", "d"),
                         new GcpProject("c", "c"));
  }
}
