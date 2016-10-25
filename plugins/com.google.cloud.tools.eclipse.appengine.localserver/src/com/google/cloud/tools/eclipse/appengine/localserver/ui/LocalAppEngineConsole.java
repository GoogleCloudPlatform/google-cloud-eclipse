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

import org.eclipse.ui.console.MessageConsole;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour;
import com.google.cloud.tools.eclipse.ui.util.MessageConsoleUtilities.ConsoleFactory;

/**
 * A console that displays information for a run/debug session of the App Engine runtime
 */
public class LocalAppEngineConsole extends MessageConsole {
  private LocalAppEngineServerBehaviour serverBehaviour;

  public LocalAppEngineConsole(String name, LocalAppEngineServerBehaviour serverBehaviour) {
    super(name, null);
    this.serverBehaviour = serverBehaviour;
  }

  public LocalAppEngineServerBehaviour getServerBehaviourDelegate() {
    return serverBehaviour;
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
