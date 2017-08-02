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

import com.google.cloud.tools.eclipse.appengine.localserver.Messages;
import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineImages;
import com.google.cloud.tools.eclipse.ui.util.MessageConsoleUtilities.ConsoleFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;

/**
 * A console that displays information for a run/debug session of the App Engine runtime
 */
public class LocalAppEngineConsole extends MessageConsole {
  private LocalAppEngineServerBehaviour serverBehaviour;
  private String unprefixedName;
  private IServerListener serverStateListener = new IServerListener() {
    @Override
    public void serverChanged(ServerEvent event) {
      if ((event.getKind() & ServerEvent.SERVER_CHANGE) != 0) {
        update(event.getState());
      }
    }
  };

  private LocalAppEngineConsole(String name, LocalAppEngineServerBehaviour serverBehaviour) {
    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2140
    // todo: setting "javaStackTraceConsole" as the console type seems distasteful
    // but is required to get stack-trace linking to work
    super(name, "javaStackTraceConsole", AppEngineImages.appEngine(16), true);
    this.unprefixedName = name;
    this.serverBehaviour = serverBehaviour;
  }

  protected void update(int serverState) {
    if (serverState == IServer.STATE_STOPPED) {
      disengage(); // we should no longer update
    }
    updateName(serverState);
  }

  /**
   * Update the shown name with the server stop/stopping state.
   */
  protected void updateName(int serverState) {
    final String computedName;
    if (serverState == IServer.STATE_STARTING) {
      computedName =
          Messages.getString("SERVER_STARTING_TEMPLATE", unprefixedName);
    } else if (serverState == IServer.STATE_STOPPING) {
      computedName =
          Messages.getString("SERVER_STOPPING_TEMPLATE", unprefixedName);
    } else if (serverState == IServer.STATE_STOPPED) {
      computedName =
          Messages.getString("SERVER_STOPPED_TEMPLATE", unprefixedName);
    } else {
      computedName = unprefixedName;
    }
    UIJob nameUpdateJob = new UIJob("Update server name") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        LocalAppEngineConsole.this.setName(computedName);
        return Status.OK_STATUS;
      }
    };
    nameUpdateJob.setSystem(true);
    nameUpdateJob.schedule();
  }

  public LocalAppEngineServerBehaviour getServerBehaviourDelegate() {
    return serverBehaviour;
  }

  @Override
  protected void init() {
    super.init();
    updateName(serverBehaviour.getServer().getServerState());
    serverBehaviour.getServer().addServerListener(serverStateListener);
  }

  @Override
  protected void dispose() {
    disengage();
    super.dispose();
  }

  /** Stop reacting to server state changes. */
  private void disengage() {
    serverBehaviour.getServer().removeServerListener(serverStateListener);
  }


  public static class Factory implements ConsoleFactory<LocalAppEngineConsole> {

    private LocalAppEngineServerBehaviour serverBehaviour;

    public Factory(LocalAppEngineServerBehaviour serverBehaviour) {
      this.serverBehaviour = serverBehaviour;
    }

    @Override
    public LocalAppEngineConsole createConsole(String name) {
      return new LocalAppEngineConsole(name, serverBehaviour);
    }

  }
}
