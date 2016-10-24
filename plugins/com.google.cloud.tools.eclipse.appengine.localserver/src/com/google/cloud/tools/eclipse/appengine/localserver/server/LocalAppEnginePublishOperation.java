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

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;

import com.google.common.collect.Lists;

/**
 * Handles the publishing operations for the App Engine development server.
 */
public class LocalAppEnginePublishOperation extends PublishOperation {
  private static final String PLUGIN_ID = LocalAppEnginePublishOperation.class.getName();

  /**
   * @throws {@link CoreException} if status list is not empty
   */
  private static void failOnError(List<IStatus> statusList) throws CoreException {
    if (statusList == null || statusList.isEmpty()) {
      return;
    }
    IStatus[] children = statusList.toArray(new IStatus[statusList.size()]);
    throw new CoreException(new MultiStatus(PLUGIN_ID, 0, children, "Error during publish operation", null));
  }

  private LocalAppEngineServerBehaviour server;
  private IModule[] modules;
  private int kind;
  private int deltaKind;
  private PublishHelper helper;

  @Override
  public int getKind() {
    return REQUIRED;
  }

  @Override
  public int getOrder() {
    return 0;
  }

  /**
   * Construct the operation object to publish the specified modules(s) to the
   * specified server.
   */
  public LocalAppEnginePublishOperation(LocalAppEngineServerBehaviour server, int kind, IModule[] modules,
      int deltaKind) {
    super("Publish to server", "Publish modules to App Engine Development Server");
    this.server = server;
    this.kind = kind;
    this.deltaKind = deltaKind;
    IPath base = server.getRuntimeBaseDirectory();
    helper = new PublishHelper(base.toFile());

    if (modules != null) {
      this.modules = Arrays.copyOf(modules, modules.length);
    } else {
      this.modules = new IModule[0];
    }
  }

  @Override
  public void execute(IProgressMonitor monitor, IAdaptable info) throws CoreException {
    List<IStatus> statusList = Lists.newArrayList();
    IPath deployPath = server.getModuleDeployDirectory(modules[0]);
    publishDirectory(deployPath, statusList, monitor);
    failOnError(statusList);
    server.setModulePublishState2(modules, IServer.PUBLISH_STATE_NONE);
  }

  /**
   * Publish modules as directory.
   */
  private void publishDirectory(IPath path, List<IStatus> statusList, IProgressMonitor monitor) {
    // delete if needed
    if (kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED) {
      File file = path.toFile();
      if (file.exists()) {
        IStatus[] status = PublishHelper.deleteDirectory(file, monitor);
        statusList.addAll(Arrays.asList(status));
      }
      // request for remove
      if (deltaKind == ServerBehaviourDelegate.REMOVED) {
        return;
      }
    }
    // republish or publish fully
    if (kind == IServer.PUBLISH_CLEAN || kind == IServer.PUBLISH_FULL) {
      IModuleResource[] resources = server.getResources(modules);
      IStatus[] publishStatus = helper.publishFull(resources, path, monitor);
      statusList.addAll(Arrays.asList(publishStatus));
      return;
    }
    // publish changes only
    IModuleResourceDelta[] deltas = server.getPublishedResourceDelta(modules);
    for (IModuleResourceDelta delta : deltas) {
      IStatus[] publishStatus = helper.publishDelta(delta, path, monitor);
      statusList.addAll(Arrays.asList(publishStatus));
    }
  }

}
