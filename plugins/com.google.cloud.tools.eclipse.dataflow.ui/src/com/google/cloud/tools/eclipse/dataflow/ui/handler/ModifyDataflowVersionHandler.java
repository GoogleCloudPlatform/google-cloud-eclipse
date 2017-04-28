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

import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowMavenModel;
import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowMavenModel.DataflowMavenModelFactory;
import com.google.cloud.tools.eclipse.dataflow.ui.DataflowUiPlugin;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import java.util.Iterator;

/**
 * A handler that updates the Dataflow Version of the selected project.
 */
public class ModifyDataflowVersionHandler extends AbstractHandler {
  private static final String NAMESPACE = "com.google.cloud.tools.eclipse.dataflow.ui.dataflowVersion.";

  private static final String PIN_COMMAND_ID = NAMESPACE + "pin";
  private static final String TRACK_COMMAND_ID = NAMESPACE + "track";
  private static final String UPDATE_COMMAND_ID = NAMESPACE + "update";

  private final DataflowMavenModelFactory modelFactory;

  public ModifyDataflowVersionHandler() {
    this(new DataflowMavenModelFactory());
  }

  @VisibleForTesting
  ModifyDataflowVersionHandler(DataflowMavenModelFactory dataflowMavenModelFactory) {
    this.modelFactory = dataflowMavenModelFactory;
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    String commandId = event.getCommand().getId();
    if (Strings.isNullOrEmpty(commandId)) {
      return null;
    }
    ISelection selection = HandlerUtil.getCurrentSelection(event);
    IProject project = getFirstProjectInSelection(selection);
    IProgressMonitor monitor = new NullProgressMonitor();

    try {
      DataflowMavenModel model = modelFactory.fromProject(project);

      switch (commandId) {
        case PIN_COMMAND_ID:
        case UPDATE_COMMAND_ID:
          model.pinDataflowDependencyToCurrent(monitor);
          return null;
        case TRACK_COMMAND_ID:
          model.trackDataflowDependency(monitor);
          return null;
        default:
          DataflowUiPlugin.logWarning(
              "Unknown Command ID %s in ModifyDataflowVersionHandler", commandId);
      }
    } catch (CoreException e) {
      throw new ExecutionException(
          "Exception while modifying Dataflow Maven Dependency for project " + project.getName(),
          e);
    }
    return null;
  }

  private IProject getFirstProjectInSelection(ISelection selection) {
    if (!(selection instanceof IStructuredSelection)) {
      // The selection doesn't contain any elements
      return null;
    }
    Iterator<?> selectionIter = ((IStructuredSelection) selection).iterator();
    while (selectionIter.hasNext()) {
      Object selected = selectionIter.next();
      if (selected instanceof IAdaptable) {
        IAdaptable adaptable = (IAdaptable) selected;
        IResource selectedResource = adaptable.getAdapter(IResource.class);
        if (selectedResource.getProject() != null) {
          return selectedResource.getProject();
        }
      }
    }
    return null;
  }
}
