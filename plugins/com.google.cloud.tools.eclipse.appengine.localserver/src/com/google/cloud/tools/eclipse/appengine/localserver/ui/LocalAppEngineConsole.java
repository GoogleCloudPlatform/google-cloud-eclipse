package com.google.cloud.tools.eclipse.appengine.localserver.ui;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour;
import com.google.cloud.tools.eclipse.ui.util.console.TaggedMessageConsole;

public class LocalAppEngineConsole extends TaggedMessageConsole<LocalAppEngineServerBehaviour> {

  public LocalAppEngineConsole(String name, LocalAppEngineServerBehaviour tag) {
    super(name, tag);
  }

}
