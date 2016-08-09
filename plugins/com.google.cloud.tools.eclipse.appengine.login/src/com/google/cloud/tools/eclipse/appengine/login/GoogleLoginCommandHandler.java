package com.google.cloud.tools.eclipse.appengine.login;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.ui.util.ServiceUtils;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

import java.io.IOException;
import java.util.Map;

public class GoogleLoginCommandHandler extends AbstractHandler implements IElementUpdater {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IGoogleLoginService loginService = ServiceUtils.getService(event, IGoogleLoginService.class);

    Credential credential = loginService.getCachedActiveCredential();
    if (credential == null) {
      credential = loginService.getActiveCredential();

      if (credential != null) {
        try {
          new GoogleLoginTemporaryTester().testLogin(credential);
          MessageDialog.openInformation(HandlerUtil.getActiveShell(event),
              "Sign-in Success", "Successfully signed in.");
        } catch (IOException ioe) {
          ErrorDialog.openError(HandlerUtil.getActiveShell(event), "Sign-in Error",
              "Sign-in failed.", StatusUtil.error(this, "See the exception detail.", ioe));
        }
      }
    } else {
      if (MessageDialog.openConfirm(HandlerUtil.getActiveShell(event),
          Messages.LOGOUT_CONFIRM_DIALOG_TITILE, Messages.LOGOUT_CONFIRM_DIALOG_MESSAGE)) {
        loginService.clearCredential();
      }
    }

    return null;
  }

  @Override
  public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
    IGoogleLoginService loginService =
        element.getServiceLocator().getService(IGoogleLoginService.class);
    boolean loggedIn = loginService.getCachedActiveCredential() != null;

    element.setText(
        loggedIn ? Messages.LOGIN_MENU_LOGGED_IN : Messages.LOGIN_MENU_LOGGED_OUT);
    element.setTooltip(
        loggedIn ? Messages.LOGIN_TOOLTIP_LOGGED_IN : Messages.LOGIN_TOOLTIP_LOGGED_OUT);
  }
}
