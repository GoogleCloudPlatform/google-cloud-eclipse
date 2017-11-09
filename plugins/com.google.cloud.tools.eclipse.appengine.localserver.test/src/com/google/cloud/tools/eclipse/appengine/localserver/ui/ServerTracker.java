/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerLifecycleListener;
import org.eclipse.wst.server.core.ServerCore;
import org.junit.rules.ExternalResource;

/** Track creation of WTP {@link IServer} instances and ensure they are deleted. */
public class ServerTracker extends ExternalResource {
  private List<IServer> servers = Collections.synchronizedList(new ArrayList<IServer>());

  private IServerLifecycleListener lifecycleListener = new IServerLifecycleListener() {
    @Override
    public void serverAdded(IServer server) {
      servers.add(server);
    }

    @Override
    public void serverChanged(IServer server) {}

    @Override
    public void serverRemoved(IServer server) {
      servers.remove(server);
    }
  };

  @Override
  protected void before() {
    ServerCore.addServerLifecycleListener(lifecycleListener);
  }

  public List<IServer> getServers() {
    return servers;
  }

  @Override
  protected void after() {
    ServerCore.removeServerLifecycleListener(lifecycleListener);
    for (IServer server : servers) {
      try {
        server.delete();
      } catch (CoreException ex) {
        /* ignore */
      }
    }
  }
}
