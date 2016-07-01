package com.google.cloud.tools.eclipse.appengine.deploy;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;

public class AppEngineStandardDeployCommandHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    new MessageDialog(HandlerUtil.getActiveShell(event), "Deploying to App Engine Standard", null,
      "The project is being deployed to App Engine Standard runtime (not really)",
      MessageDialog.INFORMATION, new String[]{"OK"}, 0).open();
    return null;
  }

  @Override
  public boolean isEnabled() {
    // TODO implement properly
    return true;
  }
}
