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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for DataflowJavaProjectNature.
 */
@RunWith(MockitoJUnitRunner.class)
public class DataflowJavaProjectNatureTest {
  @Mock
  private IProject project;
  @Mock
  private IProjectDescription description;

  @Before
  public void setup() throws CoreException {
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
    assertFalse(DataflowJavaProjectNature.hasDataflowNature(project));
  }

  @Test
  public void testHasDataflowNatureOnProjectWithDataflowNatureReturnsTrue() throws CoreException {
    when(project.hasNature(DataflowJavaProjectNature.DATAFLOW_NATURE_ID)).thenReturn(true);

    assertTrue(DataflowJavaProjectNature.hasDataflowNature(project));
  }

  @Test
  public void testAddDataflowJavaNatureToProjectThatIsNotAccessibleThrowsException() {
    when(project.isAccessible()).thenReturn(false);

    try {
      DataflowJavaProjectNature.addDataflowJavaNatureToProject(project, new NullProgressMonitor());
    } catch (CoreException ex) {
      assertEquals("Can't add the Dataflow nature to closed project null", ex.getMessage());
    }

    verify(project).isAccessible();
    verify(project).getName();
    verifyNoMoreInteractions(project);
  }

  @Test
  public void testAddDataflowJavaNatureToProjectWithoutDataflowNatureAddsNature()
      throws CoreException {
    when(description.getNatureIds()).thenReturn(new String[] {});

    DataflowJavaProjectNature.addDataflowJavaNatureToProject(project, new NullProgressMonitor());

    verify(description).setNatureIds(new String[] {DataflowJavaProjectNature.DATAFLOW_NATURE_ID});
  }

  @Test
  public void testAddDataflowJavaNatureToProjectWithDataflowNatureAlreadyPresentDoesNothing()
      throws CoreException {
    when(project.hasNature(DataflowJavaProjectNature.DATAFLOW_NATURE_ID)).thenReturn(true);

    DataflowJavaProjectNature.addDataflowJavaNatureToProject(project, new NullProgressMonitor());

    verify(project).isAccessible();
    verify(project).hasNature(DataflowJavaProjectNature.DATAFLOW_NATURE_ID);
    verifyNoMoreInteractions(description);
  }

  @Test
  public void testRemoveDataflowJavaNatureFromProjectThatIsNotAccessibleThrowsException() {
    when(project.isAccessible()).thenReturn(false);

    try {
      DataflowJavaProjectNature.removeDataflowJavaNatureFromProject(
          project, new NullProgressMonitor());
    } catch (CoreException ex) {
      assertEquals("Can't remove the Dataflow nature from closed project null", ex.getMessage());
    }

    verify(project).isAccessible();
    verify(project).getName();
    verifyNoMoreInteractions(project);
  }

  @Test
  public void testRemoveDataflowJavaNatureFromProjectWithoutDataflowNatureDoesNothing()
      throws CoreException {
    DataflowJavaProjectNature.removeDataflowJavaNatureFromProject(
        project, new NullProgressMonitor());

    verify(project).isAccessible();
    verify(project).hasNature(DataflowJavaProjectNature.DATAFLOW_NATURE_ID);
    verifyNoMoreInteractions(description);
  }

  @Test
  public void testRemoveDataflowJavaNatureFromProjectWithDataflowNaturePresentRemoves()
      throws CoreException {
    when(project.hasNature(DataflowJavaProjectNature.DATAFLOW_NATURE_ID)).thenReturn(true);
    when(description.getNatureIds())
        .thenReturn(new String[] {"foo", DataflowJavaProjectNature.DATAFLOW_NATURE_ID});

    DataflowJavaProjectNature.removeDataflowJavaNatureFromProject(
        project, new NullProgressMonitor());

    verify(description).setNatureIds(new String[] {"foo"});
  }
}
