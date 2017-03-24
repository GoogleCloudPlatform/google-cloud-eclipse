/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

/**
 * Tests for {@link ModifyDataflowNatureHandler}.
 */
public class ModifyDataflowNatureHandlerTest {
  @Mock
  private IProject project;

  @Mock
  private IProjectDescription projectDescription;

  @Mock
  private IAdaptable selectedAdaptable;

  @Mock
  private IStructuredSelection structuredSelection;

  private ModifyDataflowNatureHandler natureHandler;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    natureHandler = new ModifyDataflowNatureHandler();

    when(project.getDescription()).thenReturn(projectDescription);
    when(project.isAccessible()).thenReturn(true);

    when(structuredSelection.iterator()).thenReturn(Arrays.asList(selectedAdaptable).iterator());

  }

  @Test
  public void testHandleCommandWithUnknownNatureIdDoesNothing() throws Exception {
    natureHandler.handleCommand("foo", structuredSelection);
  }

  @Test
  public void testHandleCommandWithAddNatureAddsNatureToSelection() throws Exception {
    when(selectedAdaptable.getAdapter(IProject.class)).thenReturn(project);
    when(projectDescription.getNatureIds()).thenReturn(new String[] {});

    natureHandler.handleCommand(
        ModifyDataflowNatureHandler.ADD_NATURE_COMMAND_ID, structuredSelection);

    ArgumentCaptor<String[]> naturesCaptor = ArgumentCaptor.forClass(String[].class);
    verify(projectDescription, times(2)).getNatureIds();
    verify(projectDescription).setNatureIds(naturesCaptor.capture());
    verifyNoMoreInteractions(projectDescription);
    String[] setNatures = naturesCaptor.getValue();
    assertEquals("com.google.cloud.dataflow.DataflowJavaProjectNature", setNatures[0]);
  }

  @Test
  public void testHandleCommandWithAddNatureEmptySelectionDoesNothing() throws Exception {
    natureHandler.handleCommand(
        ModifyDataflowNatureHandler.ADD_NATURE_COMMAND_ID, mock(ISelection.class));
  }

  @Test
  public void testHandleCommandWithRemoveNatureRemovesNatureFromSelection() throws Exception {
    when(selectedAdaptable.getAdapter(IProject.class)).thenReturn(project);
    when(projectDescription.getNatureIds())
        .thenReturn(new String[] {"com.google.cloud.dataflow.DataflowJavaProjectNature"});

    natureHandler.handleCommand(
        ModifyDataflowNatureHandler.REMOVE_NATURE_COMMAND_ID, structuredSelection);

    ArgumentCaptor<String[]> naturesCaptor = ArgumentCaptor.forClass(String[].class);
    verify(projectDescription, times(2)).getNatureIds();
    verify(projectDescription).setNatureIds(naturesCaptor.capture());
    verifyNoMoreInteractions(projectDescription);
    String[] setNatures = naturesCaptor.getValue();
    assertEquals(0, setNatures.length);
  }

  @Test
  public void testHandeCommandWithRemoveNatureEmptySelectionDoesNothing() throws Exception {
    natureHandler.handleCommand(
        ModifyDataflowNatureHandler.REMOVE_NATURE_COMMAND_ID, mock(ISelection.class));
  }
}
