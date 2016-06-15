package com.google.cloud.tools.eclipse.appengine.localserver.server;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;

public class TargetPlatform {

  static void showConsole(MessageConsole console) throws PartInitException {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    if (window != null) { // TODO how to open when this is null?
      // see https://wiki.eclipse.org/FAQ_How_do_I_find_the_active_workbench_page%3F
      IWorkbenchPage page = window.getActivePage();
      IConsoleView view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
      view.display(console);
    }
  }

  static MessageConsole findConsole(String name) {
    ConsolePlugin plugin = ConsolePlugin.getDefault();
    IConsoleManager manager = plugin.getConsoleManager();
    IConsole[] consoles = manager.getConsoles();
    for (int i = 0; i < consoles.length; i++)
       if (name.equals(consoles[i].getName())) {
          return (MessageConsole) consoles[i];
       }
    // console not found, so create a new one
    MessageConsole console = new MessageConsole(name, null);
    manager.addConsoles(new IConsole[]{console});
    return console;
   }

}
