package com.google.cloud.tools.eclipse.appengine.login;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.SameShellProvider;

public class GoogleLoginCommandHandler extends AbstractHandler {

  public Object execute(ExecutionEvent event) throws ExecutionException {
    IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

    SameShellProvider shellProvider = new SameShellProvider(window.getShell());
    boolean success = new GoogleLoginTemporaryTester().testLogin(shellProvider);

    MessageDialog.openInformation(window.getShell(),
        "TESTING AUTH", success ? "SUCCESS" : "FAILURE (to be implemented)");
    return null;
  }
}
