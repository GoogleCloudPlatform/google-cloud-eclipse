package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour;
import com.google.cloud.tools.eclipse.ui.util.MessageConsoleUtilities.TaggedMessageConsoleFactory;
import com.google.cloud.tools.eclipse.ui.util.console.TaggedMessageConsole;

public class LocalAppEngineConsole extends TaggedMessageConsole<LocalAppEngineServerBehaviour> {

  public LocalAppEngineConsole(String name, LocalAppEngineServerBehaviour tag) {
    super(name, tag);
  }

  public static class Factory implements TaggedMessageConsoleFactory<LocalAppEngineConsole,
                                                                     LocalAppEngineServerBehaviour> {
    @Override
    public LocalAppEngineConsole createConsole(String name) {
      return new LocalAppEngineConsole(name, null);
    }

    @Override
    public LocalAppEngineConsole createConsole(String name, LocalAppEngineServerBehaviour tag) {
      return new LocalAppEngineConsole(name, tag);
    }
  }
}
