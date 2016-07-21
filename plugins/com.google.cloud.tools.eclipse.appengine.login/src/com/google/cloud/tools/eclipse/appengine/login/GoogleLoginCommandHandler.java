package com.google.cloud.tools.eclipse.appengine.login;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public class GoogleLoginCommandHandler extends AbstractHandler {

  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = HandlerUtil.getActiveShell(event);

    boolean success = new GoogleLoginTemporaryTester().testLogin(shell);

    MessageDialog.openInformation(shell,
        "TESTING AUTH", success ? "SUCCESS" : "FAILURE (to be implemented)");
    return null;
  }
}
