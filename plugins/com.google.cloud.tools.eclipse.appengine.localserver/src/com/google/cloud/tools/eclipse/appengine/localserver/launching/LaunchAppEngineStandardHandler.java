/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.localserver.launching;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.server.core.IModule;

/** Find or create a server with the selected projects and launch it. */
public class LaunchAppEngineStandardHandler extends AbstractHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    String launchMode = event.getParameter("launchMode");
    if (launchMode == null) {
      launchMode = ILaunchManager.DEBUG_MODE;
    }
    LaunchHelper launcher = new LaunchHelper();
    try {
      IModule[] modules = asModules(launcher, event);
      launcher.launch(modules, launchMode);
    } catch (CoreException ex) {
      throw new ExecutionException("Unable to configure server", ex);
    }
    return null;
  }

  /** Identify the relevant modules from the execution context. */
  private static IModule[] asModules(LaunchHelper launcher, ExecutionEvent event)
      throws ExecutionException {
    // First check the current selected objects
    ISelection selection = HandlerUtil.getCurrentSelection(event);
    if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
      try {
        return launcher.asModules(selection);
      } catch (CoreException e) {
        // ignore
      }
    }
    // Check the project of the active editor.
    IEditorPart editor = HandlerUtil.getActiveEditor(event);
    if (editor != null) {
      try {
        return launcher.asModules(editor);
      } catch (CoreException e) {
        // ignore
      }
    }
    throw new ExecutionException("Cannot determine server execution context");
  }


}
