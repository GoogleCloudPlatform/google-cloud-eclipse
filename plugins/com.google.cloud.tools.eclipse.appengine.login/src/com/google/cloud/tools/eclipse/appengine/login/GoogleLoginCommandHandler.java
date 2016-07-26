package com.google.cloud.tools.eclipse.appengine.login;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

import com.google.api.client.auth.oauth2.Credential;

public class GoogleLoginCommandHandler extends AbstractHandler implements IElementUpdater {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = HandlerUtil.getActiveShell(event);
    GoogleLoginService loginService = new GoogleLoginService();

    Credential credential = loginService.getCachedActiveCredential();
    if (credential == null) {
      try {
        credential = loginService.getActiveCredential(new SameShellProvider(shell));

        boolean success = new GoogleLoginTemporaryTester().testLogin(credential);
        MessageDialog.openInformation(shell,
            "TESTING AUTH", success ? "SUCCESS" : "FAILURE (to be implemented)");
      } catch (IOException ioe) {
        throw new ExecutionException(ioe.getMessage());
      }
    } else {
      if (MessageDialog.openConfirm(shell,
          Messages.LOGOUT_CONFIRM_DIALOG_TITILE, Messages.LOGOUT_CONFIRM_DIALOG_MESSAGE)) {
        loginService.clearCredential();
      }
    }

    ICommandService commandService =
        (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
    commandService.refreshElements(
        "com.google.cloud.tools.eclipse.appengine.login.commands.loginCommand", null); //$NON-NLS-1$

    return null;
  }

  @Override
  public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
    boolean loggedIn = new GoogleLoginService().getCachedActiveCredential() != null;

    element.setText(
        loggedIn ? Messages.LOGIN_MENU_LOGGED_IN : Messages.LOGIN_MENU_LOGGED_OUT);
    element.setTooltip(
        loggedIn ? Messages.LOGIN_TOOLTIP_LOGGED_IN : Messages.LOGIN_TOOLTIP_LOGGED_OUT);
  }
}
