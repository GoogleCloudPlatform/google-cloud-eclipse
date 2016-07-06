package com.google.cloud.tools.eclipse.appengine.deploy;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;

public class AppEngineStandardDeployCommandHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    new MessageDialog(HandlerUtil.getActiveShell(event), Messages.getString("deploy.standard.dialog.title"), null, //$NON-NLS-1$
      Messages.getString("deploy.standard.dialog.message"), //$NON-NLS-1$
      MessageDialog.INFORMATION, new String[]{Messages.getString("button.ok.label")}, 0).open(); //$NON-NLS-1$
    return null;
  }

  @Override
  public boolean isEnabled() {
    // TODO implement properly
    return true;
  }
}
