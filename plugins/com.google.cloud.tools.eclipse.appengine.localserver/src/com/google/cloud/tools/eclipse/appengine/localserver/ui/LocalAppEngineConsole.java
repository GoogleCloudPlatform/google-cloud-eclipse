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
