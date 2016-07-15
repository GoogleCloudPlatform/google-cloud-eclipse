package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import org.eclipse.ui.console.MessageConsole;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour;

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
}
