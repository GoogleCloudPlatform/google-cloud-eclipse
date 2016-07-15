package com.google.cloud.tools.eclipse.appengine.localserver.server;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import com.google.cloud.tools.eclipse.appengine.localserver.ui.LocalAppEngineConsole;

public class ConsoleUtilities {
  static LocalAppEngineConsole findConsole(String name, LocalAppEngineServerBehaviour serverBehaviour) {
    ConsolePlugin plugin = ConsolePlugin.getDefault();
    IConsoleManager manager = plugin.getConsoleManager();
    IConsole[] consoles = manager.getConsoles();
    for (int i = 0; i < consoles.length; i++)
       if (name.equals(consoles[i].getName())) {
          return (LocalAppEngineConsole) consoles[i];
       }
    // console not found, so create a new one
    LocalAppEngineConsole console = new LocalAppEngineConsole(name, serverBehaviour);
    manager.addConsoles(new IConsole[]{console});
    return console;
   }

}
