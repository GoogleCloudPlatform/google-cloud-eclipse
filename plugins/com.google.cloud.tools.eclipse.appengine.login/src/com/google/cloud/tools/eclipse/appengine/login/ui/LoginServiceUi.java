/*******************************************************************************
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.login.ui;

import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.cloud.tools.eclipse.appengine.login.GoogleLoginService;
import com.google.cloud.tools.eclipse.appengine.login.Messages;
import com.google.cloud.tools.ide.login.UiFacade;
import com.google.cloud.tools.ide.login.VerificationCodeHolder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.services.IServiceLocator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginServiceUi implements UiFacade {

  private static final Logger logger = Logger.getLogger(LoginServiceUi.class.getName());

  private static final String ERROR_MARKER_USER_CANCELLED_LOGIN = "cancelled-by-user"; //$NON-NLS-1$

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
    showErrorDialogHelper(Messages.LOGIN_ERROR_DIALOG_TITLE, Messages.LOGIN_ERROR_DIALOG_MESSAGE);
  }

  @Override
  public void notifyStatusIndicator() {
    // Update and refresh the menu, toolbar button, and tooltip.
    display.asyncExec(new Runnable() {
      @Override
      public void run() {
        serviceLocator.getService(ICommandService.class).refreshElements(
            "com.google.cloud.tools.eclipse.appengine.login.commands.loginCommand", //$NON-NLS-1$
            null);
      }
    });
  }

  @Override
  public VerificationCodeHolder obtainVerificationCodeFromExternalUserInteraction(String title) {
    LocalServerReceiver codeReceiver = new LocalServerReceiver();

    try {
      String redirectUrl = codeReceiver.getRedirectUri();
      if (!Program.launch(GoogleLoginService.getGoogleLoginUrl(redirectUrl))) {
        showErrorDialogHelper(
            Messages.LOGIN_ERROR_DIALOG_TITLE, Messages.LOGIN_ERROR_CANNOT_OPEN_BROWSER);
        return null;
      }

      String authorizationCode = showProgressDialogAndWaitForCode(codeReceiver, redirectUrl);
      if (authorizationCode != null) {
        return new VerificationCodeHolder(authorizationCode, redirectUrl);
      }
      return null;

    } catch (IOException ioe) {
      // Don't show an error dialog if a user pressed the cancel button.
      if (!ioe.getMessage().contains(ERROR_MARKER_USER_CANCELLED_LOGIN)) {
        showErrorDialogHelper(Messages.LOGIN_ERROR_DIALOG_TITLE,
            Messages.LOGIN_ERROR_LOCAL_SERVER_RUN + ioe.getCause().getLocalizedMessage());
      }
      return null;
    }
  }

  private String showProgressDialogAndWaitForCode(
      final LocalServerReceiver codeReceiver, final String redirectUrl) throws IOException {
    final Semaphore checkingIn = new Semaphore(0);  // Ensures memory consistency between threads.

    final ProgressMonitorDialog dialog = new ProgressMonitorDialog(shellProvider.getShell()) {
      @Override
      protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.LOGIN_PROGRESS_DIALOG_TITLE);
      }
      @Override
      protected void cancelPressed() {
        checkingIn.release();  // Self unlocking when a user pressed the cancel button.
        super.cancelPressed();
      }
    };
    dialog.setBlockOnOpen(true);
    dialog.setCancelable(true);
    dialog.create();
    dialog.getProgressMonitor()
        .beginTask(Messages.LOGIN_PROGRESS_DIALOG_MESSAGE, IProgressMonitor.UNKNOWN);

    String[] codeHolder = new String[1];
    IOException[] exceptionHolder = new IOException[1];

    scheduleCodeWaitingJob(codeReceiver, dialog, checkingIn, codeHolder, exceptionHolder);
    dialog.open();  // Blocked, but not by the job. Resumes as soon as the dialog closes.
    stopCodeWaitingJob(redirectUrl);
    checkingIn.acquireUninterruptibly();  // Synchronize here for memory consistency.

    if (exceptionHolder[0] != null) {
      throw exceptionHolder[0];
    }
    return codeHolder[0];
  }

  // We don't use the ProgressMonitorDialog's built-in support for running a job
  // (ProgressMonitorDialog.run()), because the dialog will be blocked forever (regardless of
  // closing the dialog) if the job fails to terminate. This method is a workaround as we don't
  // have 100% guarantee that we can cancel LocalServerReceiver.waitForCode().
  //
  // However, under normal circumstances, stopCodeWaitingJob() will succeed and this job will
  // terminate.
  private void scheduleCodeWaitingJob(
      final LocalServerReceiver codeReceiver, final ProgressMonitorDialog dialog,
      final Semaphore checkingIn, final String[] codeHolder, final IOException[] exceptionHolder) {
    final Display display = dialog.getShell().getDisplay();
    Job codeWaitingJob = new Job("Waiting for Authorization Code") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          codeHolder[0] = codeReceiver.waitForCode();
        } catch (IOException ioe) {
          exceptionHolder[0] = ioe;
        }
        finally {
          try {
            checkingIn.release();  // Ensure the UI thread can see the writes from this thread.
            codeReceiver.stop();
          } catch (IOException ioe) {
            logger.log(Level.WARNING, "Failed to stop the local web server.", ioe); //$NON-NLS-1$
          }

          // Close the progress dialog so that Eclipse can move forward.
          display.syncExec(new Runnable() {
            @Override
            public void run() {
              dialog.close();
            }
          });
        }
        return Status.OK_STATUS;
      }
    };
    codeWaitingJob.setSystem(true);  // Hide the job from UI.
    codeWaitingJob.schedule();
  }

  /**
   * Stops the background task inside {@link showProgressDialogAndWaitForCode} by sending a login
   * error (as an HTTP request} to the local server. {@link LocalServerReceiver#waitForCode} will
   * subsequently throw an {@link IOException}.
   */
  private void stopCodeWaitingJob(final String redirectUrl) {
    // Wrap in a Job for the case where making HTTP connections takes a long time.
    new Job("Terminate Authorization Code Receiver") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        HttpURLConnection connection = null;

        try {
          URL url = new URL(redirectUrl + "?error=" + ERROR_MARKER_USER_CANCELLED_LOGIN); //$NON-NLS-1$
          connection = (HttpURLConnection) url.openConnection();
          int responseCode = connection.getResponseCode();
          if (responseCode != HttpURLConnection.HTTP_OK) {
            logger.log(Level.WARNING, "Error terminating local server. Response: " + responseCode); //$NON-NLS-1$
          }
        } catch (IOException ex) {
          logger.log(Level.WARNING, "Error terminating local server", ex); //$NON-NLS-1$
        } finally {
          if (connection != null) {
            connection.disconnect();
          }
        }
        return Status.OK_STATUS;
      }
    }.schedule();
  }

  @Override
  public String obtainVerificationCodeFromUserInteraction(
      String title, GoogleAuthorizationCodeRequestUrl authCodeRequestUrl) {
    throw new RuntimeException("Not to be called."); //$NON-NLS-1$
  }
}
