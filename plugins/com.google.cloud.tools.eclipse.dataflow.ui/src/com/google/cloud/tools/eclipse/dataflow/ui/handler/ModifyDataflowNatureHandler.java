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

import com.google.cloud.tools.eclipse.dataflow.core.natures.DataflowJavaProjectNature;
import com.google.cloud.tools.eclipse.dataflow.ui.DataflowUiPlugin;
import com.google.cloud.tools.eclipse.ui.util.ProjectFromSelectionHelper;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * A handler that updates the Dataflow nature of a selected project.
 */
public class ModifyDataflowNatureHandler extends AbstractHandler {
  private static final String NAMESPACE = "com.google.cloud.tools.eclipse.dataflow.ui.dataflowNature.";

  @VisibleForTesting
  static final String ADD_NATURE_COMMAND_ID = NAMESPACE + "add";

  @VisibleForTesting
  static final String REMOVE_NATURE_COMMAND_ID = NAMESPACE + "remove";

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    List<IProject> projects = ProjectFromSelectionHelper.getProjects(event);
    return handleCommand(event.getCommand().getId(), projects);
  }

  @VisibleForTesting
  Object handleCommand(String commandId, List<IProject> projects) {
    switch (commandId) {
      case ADD_NATURE_COMMAND_ID:
        setDataflowNatureOfSelection(projects, true);
        break;
      case REMOVE_NATURE_COMMAND_ID:
        setDataflowNatureOfSelection(projects, false);
        break;
      default:
        DataflowUiPlugin.logWarning(
            "Modify Dataflow Nature Handler without recognized command %s", commandId);
    }
    return null;
  }

  private void setDataflowNatureOfSelection(List<IProject> projects, boolean enable) {
    for (IProject project : projects) {
      try {
        if (enable) {
          DataflowJavaProjectNature
              .addDataflowJavaNatureToProject(project, new NullProgressMonitor());
        } else {
          DataflowJavaProjectNature
              .removeDataflowJavaNatureFromProject(project, new NullProgressMonitor());
        }
      } catch (CoreException e) {
        DataflowUiPlugin.logError(e,
            "Exception while trying to remove Dataflow Nature from Project %s", project);
      }
    }
  }
}
