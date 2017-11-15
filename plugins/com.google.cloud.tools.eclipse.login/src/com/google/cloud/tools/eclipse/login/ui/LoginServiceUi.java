/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.login.ui;

import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.cloud.tools.eclipse.login.GoogleLoginService;
import com.google.cloud.tools.eclipse.login.Messages;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsEvents;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager;
import com.google.cloud.tools.login.UiFacade;
import com.google.cloud.tools.login.VerificationCodeHolder;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.services.IServiceLocator;

public class LoginServiceUi implements UiFacade {

  private static final Logger logger = Logger.getLogger(LoginServiceUi.class.getName());

  private IServiceLocator serviceLocator;
  private IShellProvider shellProvider;
  private Display display;

  public LoginServiceUi(IServiceLocator serviceLocator, IShellProvider shellProvider,
      Display display) {
    this.serviceLocator = serviceLocator;
    this.shellProvider = shellProvider;
    this.display = display;
  }

  public void showErrorDialogHelper(String title, String message) {
    MessageDialog.openError(shellProvider.getShell(), title, message);
  }

  @Override
  public boolean askYesOrNo(String title, String message) {
    throw new RuntimeException("Not allowed to ensure non-UI threads don't prompt."); //$NON-NLS-1$
  }

  @Override
  public void showErrorDialog(String title, String message) {
    // Ignore "title" and "message", as they are non-localized hard-coded strings in the library.
    showErrorDialogHelper(Messages.getString("LOGIN_ERROR_DIALOG_TITLE"),
        Messages.getString("LOGIN_ERROR_DIALOG_MESSAGE"));
  }

  @Override
  public void notifyStatusIndicator() {
    // Update and refresh the menu, toolbar button, and tooltip.
    display.asyncExec(new Runnable() {
      @Override
      public void run() {
        serviceLocator.getService(ICommandService.class).refreshElements(
            "com.google.cloud.tools.eclipse.login.commands.loginCommand", //$NON-NLS-1$
            null);
      }
    });
  }

  @Override
  public VerificationCodeHolder obtainVerificationCodeFromExternalUserInteraction(String message) {
    LocalServerReceiver codeReceiver = new LocalServerReceiver();

    try {
      String redirectUrl = codeReceiver.getRedirectUri();
      if (!Program.launch(GoogleLoginService.getGoogleLoginUrl(redirectUrl))) {
        showErrorDialogHelper(
            Messages.getString("LOGIN_ERROR_DIALOG_TITLE"),
            Messages.getString("LOGIN_ERROR_CANNOT_OPEN_BROWSER"));
        return null;
      }

      String authorizationCode = showProgressDialogAndWaitForCode(message, codeReceiver);
      if (authorizationCode != null) {
        AnalyticsPingManager.getInstance().sendPingOnShell(shellProvider.getShell(),
            AnalyticsEvents.LOGIN_SUCCESS);

        return new VerificationCodeHolder(authorizationCode, redirectUrl);
      }
      return null;

    } catch (IOException ioe) {
      showErrorDialogHelper(Messages.getString("LOGIN_ERROR_DIALOG_TITLE"),
          Messages.getString("LOGIN_ERROR_LOCAL_SERVER_RUN", ioe.getLocalizedMessage()));
      return null;
    } finally {
      stopLocalServerReceiver(codeReceiver);
    }
  }

  private String showProgressDialogAndWaitForCode(final String message,
      final LocalServerReceiver codeReceiver) throws IOException {
    try {
      final ProgressMonitorDialog dialog = new ProgressMonitorDialog(shellProvider.getShell()) {
        @Override
        protected void configureShell(Shell shell) {
          super.configureShell(shell);
          shell.setText(Messages.getString("LOGIN_PROGRESS_DIALOG_TITLE"));
        }
        @Override
        protected void cancelPressed() {
          stopLocalServerReceiver(codeReceiver);

          AnalyticsPingManager.getInstance().sendPingOnShell(getParentShell(),
              AnalyticsEvents.LOGIN_CANCELED);
        }
      };

      final String[] codeHolder = new String[1];
      dialog.run(true /* fork */, true /* cancelable */, new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
          AnalyticsPingManager.getInstance().sendPingOnShell(dialog.getShell(),
              AnalyticsEvents.LOGIN_START);

          monitor.beginTask(
              message != null ? message : Messages.getString("LOGIN_PROGRESS_DIALOG_MESSAGE"),
              IProgressMonitor.UNKNOWN);
          try {
            codeHolder[0] = codeReceiver.waitForCode();
          } catch (IOException ioe) {
            throw new InvocationTargetException(ioe);
          }
        }
      });
      return codeHolder[0];

    } catch (InvocationTargetException ex) {
      throw (IOException) ex.getTargetException();
    } catch (InterruptedException ex) {  // Never thrown from the attached task.
      return null;
    }
  }

  private void stopLocalServerReceiver(LocalServerReceiver codeReceiver) {
    try {
      codeReceiver.stop();
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Failed to stop the local web server for login.", ex); //$NON-NLS-1$
    }
  }

  @Override
  public String obtainVerificationCodeFromUserInteraction(
      String title, GoogleAuthorizationCodeRequestUrl authCodeRequestUrl) {
    throw new RuntimeException("Not to be called."); //$NON-NLS-1$
  }
}
