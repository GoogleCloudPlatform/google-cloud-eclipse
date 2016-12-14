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

import com.google.cloud.tools.eclipse.jst.server.core.BasePublishOperation;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;

/**
 * Handles the publishing operations for the App Engine development server.
 */
public class LocalAppEnginePublishOperation extends BasePublishOperation {

  private LocalAppEngineServerBehaviour server;

  /**
   * Construct the operation object to publish the specified module(s) to the
   * specified server.
   */
  public LocalAppEnginePublishOperation(LocalAppEngineServerBehaviour server, int kind,
      IModule[] modules, int deltaKind) {
    super("Publish to server", "Publish module to App Engine Development Server", kind, modules,
        deltaKind);
    this.server = server;
  }

  @Override
  protected IPath getRuntimeBaseDirectory() {
    return server.getRuntimeBaseDirectory();
  }

  @Override
  protected IPath getModuleDeployDirectory(IModule module) {
    return server.getModuleDeployDirectory(module);
  }

  @Override
  protected void setModulePublishState(IModule[] module, int publishState) {
    server.setModulePublishState2(module, publishState);
  }

  @Override
  protected IModuleResourceDelta[] getPublishedResourceDelta(IModule[] module) {
    return server.getPublishedResourceDelta(module);
  }

  @Override
  protected IModuleResource[] getResources(IModule[] module) {
    return server.getResources(module);
  }

  @Override
  protected boolean isServeModulesWithoutPublish() {
    return false;
  }

}
