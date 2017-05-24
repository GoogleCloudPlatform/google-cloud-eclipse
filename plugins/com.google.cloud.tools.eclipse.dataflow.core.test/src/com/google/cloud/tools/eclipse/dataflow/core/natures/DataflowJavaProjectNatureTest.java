/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.natures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for DataflowJavaProjectNature.
 */
@RunWith(JUnit4.class)
public class DataflowJavaProjectNatureTest {
  @Mock
  private IProject project;
  @Mock
  private IProjectDescription description;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() throws CoreException {
    MockitoAnnotations.initMocks(this);
    when(project.isAccessible()).thenReturn(true);
    when(project.getDescription()).thenReturn(description);
  }

  @Test
  public void testHasDataflowNatureOnProjectThatDoesNotExistDoesNotInteractAndReturnsFalse()
      throws CoreException {
    when(project.isAccessible()).thenReturn(false);
    boolean hasNature = DataflowJavaProjectNature.hasDataflowNature(project);

    assertFalse(hasNature);

    verify(project).isAccessible();
    verifyNoMoreInteractions(project);
  }

  @Test
  public void testHasDataflowNatureOnProjectWithoutDataflowNatureReturnsFalse()
      throws CoreException {
    when(description.getNatureIds()).thenReturn(new String[0]);

    assertFalse(DataflowJavaProjectNature.hasDataflowNature(project));
  }

  @Test
  public void testHasDataflowNatureOnProjectWithDataflowNatureReturnsTrue() throws CoreException {
    when(description.getNatureIds()).thenReturn(
        new String[] {DataflowJavaProjectNature.DATAFLOW_NATURE_ID});

    assertTrue(DataflowJavaProjectNature.hasDataflowNature(project));
  }

  @Test
  public void testAddDataflowJavaNatureToProjectThatIsNotAccessibleThrowsException()
      throws CoreException {
    when(project.isAccessible()).thenReturn(false);

    thrown.expect(CoreException.class);
    thrown.expectMessage("nonexistent or closed project");
    thrown.expectMessage("add the Dataflow nature");

    DataflowJavaProjectNature.addDataflowJavaNatureToProject(project, new NullProgressMonitor());

    verify(project).isAccessible();
    verifyNoMoreInteractions(project);
  }

  @Test
  public void testAddDataflowJavaNatureToProjectWithoutDataflowNatureAddsNature()
      throws CoreException {
    when(description.getNatureIds()).thenReturn(new String[] {});

    DataflowJavaProjectNature.addDataflowJavaNatureToProject(project, new NullProgressMonitor());

    ArgumentCaptor<String[]> natureCaptor = ArgumentCaptor.forClass(String[].class);
    verify(description).setNatureIds(natureCaptor.capture());
    String[] setNatures = natureCaptor.getValue();
    assertEquals(1, setNatures.length);
    assertEquals(DataflowJavaProjectNature.DATAFLOW_NATURE_ID, setNatures[0]);
  }

  @Test
  public void testAddDataflowJavaNatureToProjectWithDataflowNatureAlreadyPresentDoesNothing()
      throws CoreException {
    when(description.getNatureIds()).thenReturn(
        new String[] {DataflowJavaProjectNature.DATAFLOW_NATURE_ID});

    DataflowJavaProjectNature.addDataflowJavaNatureToProject(project, new NullProgressMonitor());

    verify(description).getNatureIds();
    verifyNoMoreInteractions(description);
  }

  @Test
  public void testRemoveDataflowJavaNatureFromProjectThatIsNotAccessibleThrowsException()
      throws CoreException {
    when(project.isAccessible()).thenReturn(false);

    thrown.expect(CoreException.class);
    thrown.expectMessage("nonexistent or closed project");
    thrown.expectMessage("remove the Dataflow nature");

    DataflowJavaProjectNature.removeDataflowJavaNatureFromProject(
        project, new NullProgressMonitor());

    verify(project).isAccessible();
    verifyNoMoreInteractions(project);
  }

  @Test
  public void testRemoveDataflowJavaNatureFromProjectWithoutDataflowNatureDoesNothing()
      throws CoreException {
    when(description.getNatureIds()).thenReturn(new String[] {});

    DataflowJavaProjectNature.removeDataflowJavaNatureFromProject(
        project, new NullProgressMonitor());

    verify(description).getNatureIds();
    verifyNoMoreInteractions(description);

  }

  @Test
  public void testRemoveDataflowJavaNatureFromProjectWithDataflowNaturePresentRemoves()
      throws CoreException {
    when(description.getNatureIds())
        .thenReturn(new String[] {"foo", DataflowJavaProjectNature.DATAFLOW_NATURE_ID});

    DataflowJavaProjectNature.removeDataflowJavaNatureFromProject(
        project, new NullProgressMonitor());

    ArgumentCaptor<String[]> natureCaptor = ArgumentCaptor.forClass(String[].class);
    verify(description).setNatureIds(natureCaptor.capture());
    String[] setNatures = natureCaptor.getValue();
    assertEquals(1, setNatures.length);
    assertEquals("foo", setNatures[0]);
  }
}
