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

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

public class DevAppServerRuntimeProcess extends RuntimeProcess {

  public DevAppServerRuntimeProcess(ILaunch launch,
                                    Process process,
                                    String name,
                                    Map<String, String> attributes) {
    super(launch, process, name, attributes);
  }

  @Override
  public void terminate() throws DebugException {
    try {
      sendQuitRequest();
      if (!getLaunch().isTerminated()) {
        super.terminate();
      }
    } catch (CoreException e) {
      throw new DebugException(e.getStatus());
    }

  }

  private void sendQuitRequest() throws CoreException {
    final IServer server = ServerUtil.getServer(getLaunch().getLaunchConfiguration());
    if (server == null) {
      return;
    }
    server.stop(true);
    try {
      // the stop command is async, let's give it some time to execute
      Thread.sleep(2000L);
    } catch (InterruptedException e) {
    }
  }

}
