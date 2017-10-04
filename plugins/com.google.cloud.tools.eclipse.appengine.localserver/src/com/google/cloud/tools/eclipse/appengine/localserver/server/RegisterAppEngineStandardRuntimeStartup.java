/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.IStartup;

/**
 * Registers the default instance of our App Engine Standard runtime if not found. Uses the internal
 * WTP {@link IStartup} to avoid unnecessary activation until use of WTP-related functionality.
 */
public class RegisterAppEngineStandardRuntimeStartup implements IStartup {
  private static final Logger logger =
      Logger.getLogger(RegisterAppEngineStandardRuntimeStartup.class.getName());

  /**
   * AppEngineStandardFacet#createAppEngineServerRuntime() creates the default runtime using a
   * {@code null} ID, which causes it to take the name of its runtime-type.
   */
  private static final String RUNTIME_ID = "App Engine Standard Runtime";

  @Override
  public void startup() {
    IRuntime runtime = ServerCore.findRuntime(RUNTIME_ID);
    if (runtime != null) {
      return;
    }
    try {
      AppEngineStandardFacet.createAppEngineServerRuntime(new NullProgressMonitor());
    } catch (CoreException ex) {
      logger.log(Level.SEVERE, "Unable to create default runtime instance", ex);
    }
  }
}
