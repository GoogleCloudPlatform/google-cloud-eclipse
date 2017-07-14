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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for {@link ModifyDataflowNatureHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ModifyDataflowNatureHandlerTest {
  @Mock
  private IProject project;

  @Mock
  private IProjectDescription projectDescription;

  private final ModifyDataflowNatureHandler natureHandler = new ModifyDataflowNatureHandler();

  @Before
  public void setup() throws Exception {
    when(project.getDescription()).thenReturn(projectDescription);
    when(project.isAccessible()).thenReturn(true);
  }

  @Test
  public void testHandleCommandWithUnknownNatureIdDoesNothing() {
    natureHandler.handleCommand("foo", Arrays.asList(project));
    verifyNoMoreInteractions(projectDescription);
  }

  @Test
  public void testHandleCommandWithAddNatureAddsNatureToSelection() {
    when(projectDescription.getNatureIds()).thenReturn(new String[] {});

    natureHandler.handleCommand(
        ModifyDataflowNatureHandler.ADD_NATURE_COMMAND_ID, Arrays.asList(project));

    verify(projectDescription)
       .setNatureIds(new String[] {"com.google.cloud.dataflow.DataflowJavaProjectNature"});
  }

  @Test
  public void testHandleCommandWithAddNatureEmptySelectionDoesNothing() {
    natureHandler.handleCommand(
        ModifyDataflowNatureHandler.ADD_NATURE_COMMAND_ID, new ArrayList<IProject>());
    verifyNoMoreInteractions(projectDescription);
  }

  @Test
  public void testHandleCommandWithRemoveNatureRemovesNatureFromSelection() throws CoreException {
    when(project.hasNature("com.google.cloud.dataflow.DataflowJavaProjectNature")).thenReturn(true);
    when(projectDescription.getNatureIds())
        .thenReturn(new String[] {"com.google.cloud.dataflow.DataflowJavaProjectNature"});

    natureHandler.handleCommand(
        ModifyDataflowNatureHandler.REMOVE_NATURE_COMMAND_ID, Arrays.asList(project));

    verify(projectDescription).setNatureIds(new String[0]);
  }

  @Test
  public void testHandeCommandWithRemoveNatureEmptySelectionDoesNothing() {
    natureHandler.handleCommand(
        ModifyDataflowNatureHandler.REMOVE_NATURE_COMMAND_ID, new ArrayList<IProject>());
    verifyNoMoreInteractions(projectDescription);
  }
}
