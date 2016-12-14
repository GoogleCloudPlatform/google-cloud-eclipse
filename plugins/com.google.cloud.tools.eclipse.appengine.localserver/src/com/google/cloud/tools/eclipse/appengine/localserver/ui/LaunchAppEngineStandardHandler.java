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

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerDelegate;
import com.google.cloud.tools.eclipse.util.AdapterUtil;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;

/** Find or create a server with the selected projects and launch it. */
public class LaunchAppEngineStandardHandler extends AbstractHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    String launchMode = event.getParameter("launchMode");
    if (launchMode == null) {
      launchMode = ILaunchManager.DEBUG_MODE;
    }
    IModule[] modules = asModules(event);
    SubMonitor progress = SubMonitor.convert(null, 10);
    try {
      IServer server = findExistingServer(modules, progress.newChild(3));
      if (server != null && isRunning(server)) {
        ILaunch launch = server.getLaunch();
        Preconditions.checkNotNull(launch, "Running server should have a launch");
        String detail = launchMode.equals(launch.getLaunchMode())
            ? "Server is already running"
            : MessageFormat.format("Server is already running in \"{0}\" mode",
                launch.getLaunchMode());
        IStatus status = StatusUtil.info(this,
            MessageFormat.format("\"{0}\" already running", server.getName()));
        throw new ExecutionException(detail, new CoreException(status));
      } else if (server == null) {
        server = createServer(modules, progress.newChild(3));
      }
      launch(server, launchMode, progress.newChild(4));
    } catch (CoreException ex) {
      throw new ExecutionException("Unable to configure server", ex);
    }
    return null;
  }

  private static boolean isRunning(IServer server) {
    return server.getServerState() == IServer.STATE_STARTED
        || server.getServerState() == IServer.STATE_STARTING;
  }

  @VisibleForTesting
  protected IServer findExistingServer(IModule[] modules, SubMonitor progress) {
    if (modules.length == 1) {
      IServer defaultServer = ServerCore.getDefaultServer(modules[0]);
      if (defaultServer != null && LocalAppEngineServerDelegate.SERVER_TYPE_ID
          .equals(defaultServer.getServerType().getId())) {
        return defaultServer;
      }
    }
    Set<IModule> myModules = ImmutableSet.copyOf(modules);
    // Look for servers that contain these modules
    // Could prioritize servers that have *exactly* these modules,
    // or that have the smallest overlap
    for (IServer server : ServerCore.getServers()) {
      if (!LocalAppEngineServerDelegate.SERVER_TYPE_ID.equals(server.getServerType().getId())) {
        continue;
      }
      Set<IModule> serverModules = ImmutableSet.copyOf(server.getModules());
      if (Sets.intersection(myModules, serverModules).size() == myModules.size()) {
        return server;
      }
    }
    return null;
  }

  private IServer createServer(IModule[] modules, SubMonitor progress) throws CoreException {
    IServerType serverType = ServerCore.findServerType(LocalAppEngineServerDelegate.SERVER_TYPE_ID);
    IServerWorkingCopy serverWorkingCopy =
        serverType.createServer(null, null, progress.newChild(4));
    serverWorkingCopy.modifyModules(modules, null, progress.newChild(4));
    return serverWorkingCopy.save(false, progress.newChild(2));
  }

  @VisibleForTesting
  protected void launch(IServer server, String launchMode, SubMonitor progress)
      throws CoreException {
    server.start(launchMode, progress);
  }

  /** Identify the relevant modules from the execution context. */
  private static IModule[] asModules(ExecutionEvent event) throws ExecutionException {
    // First check the current selected objects
    ISelection selection = HandlerUtil.getCurrentSelection(event);
    if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
      Object[] selectedObjects = ((IStructuredSelection) selection).toArray();
      List<IModule> modules = new ArrayList<>(selectedObjects.length);
      for (Object object : selectedObjects) {
        modules.add(asModule(object));
      }
      return modules.toArray(new IModule[modules.size()]);
    }
    // Check the project of the active editor.
    IEditorPart editor = HandlerUtil.getActiveEditor(event);
    if (editor != null && editor.getEditorInput() instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) editor.getEditorInput();
      IProject project = input.getFile().getProject();
      if (project != null) {
        return new IModule[] {asModule(project)};
      }
    }
    throw new ExecutionException("Cannot determine server execution context");
  }

  private static IModule asModule(Object object) throws ExecutionException {
    IModule module = AdapterUtil.adapt(object, IModule.class);
    if (module != null) {
      return module;
    }
    IProject project = AdapterUtil.adapt(object, IProject.class);
    if (project != null) {
      module = ServerUtil.getModule(project);
      if (module != null) {
        return module;
      }
    }
    throw new ExecutionException("no module found for " + object);
  }

}
