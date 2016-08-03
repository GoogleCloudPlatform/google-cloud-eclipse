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

package com.google.cloud.tools.eclipse.appengine.login;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.login.ui.LoginServiceUi;
import com.google.cloud.tools.ide.login.GoogleLoginState;
import com.google.cloud.tools.ide.login.LoggerFacade;
import com.google.cloud.tools.ide.login.OAuthDataStore;
import com.google.cloud.tools.ide.login.UiFacade;
import com.google.common.annotations.VisibleForTesting;

import org.eclipse.e4.core.contexts.IEclipseContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides service related to login, e.g., account management, getting a credential of a
 * currently active user, etc.
 */
public class GoogleLoginService implements IGoogleLoginService {

  // For the detailed info about each scope, see
  // https://github.com/GoogleCloudPlatform/gcloud-eclipse-tools/wiki/Cloud-Tools-for-Eclipse-Technical-Design#oauth-20-scopes-requested
  private static final SortedSet<String> OAUTH_SCOPES = Collections.unmodifiableSortedSet(
      new TreeSet<>(Arrays.asList(
          "email", //$NON-NLS-1$
          "https://www.googleapis.com/auth/cloud-platform" //$NON-NLS-1$
      )));

  private GoogleLoginState loginState;
  private AtomicBoolean loginInProgress;

  @VisibleForTesting
  GoogleLoginService(
      OAuthDataStore dataStore, UiFacade uiFacade, LoggerFacade loggerFacade) {
    loginState = new GoogleLoginState(
        Constants.getOAuthClientId(), Constants.getOAuthClientSecret(), OAUTH_SCOPES,
        dataStore, uiFacade, loggerFacade);
    loginInProgress = new AtomicBoolean(false);
  }

  static IGoogleLoginService createDefaultInstance(IEclipseContext eclipseContext) {
    return new GoogleLoginService(new TransientOAuthDataStore(eclipseContext),
        new LoginServiceUi(), new LoginServiceLogger());
  }

  /**
   * Returns the credential of an active user (among multiple logged-in users). A login screen
   * may be presented, e.g., if no user is logged in or login is required due to an expired
   * credential. This method returns {@code null} if a user cancels the login process.
   * For this reason, if {@code null} is returned, the caller should cancel the current
   * operation and display a general message that login is required but was cancelled or failed.
   *
   * Must be called from a UI context.
   */
  @Override
  public Credential getActiveCredential() {
    if (!loginInProgress.compareAndSet(false, true)) {
      LoginServiceUi.showErrorDialogHelper(
          Messages.LOGIN_ERROR_DIALOG_TITLE, Messages.LOGIN_ERROR_IN_PROGRESS);
    }

    // TODO: holding a lock for a long period of time (especially when waiting for UI events)
    // should be avoided. Make the login library thread-safe, and don't lock during UI events.
    // As a workaround and temporary relief, we use the loginInProgress flag above to fail
    // conservatively if login seems to be in progress.
    try {
      synchronized (loginState) {
        if (loginState.logIn(null /* parameter ignored */)) {
          return loginState.getCredential();
        }
        return null;
      }
    }
    finally {
      loginInProgress.set(false);
    }
  }

  /**
   * Returns the credential of an active user (among multiple logged-in users). Unlike {@link
   * #getActiveCredential}, this version does not involve login process or make API calls.
   * Returns {@code null} if no credential has been cached.
   *
   * Safe to call from non-UI contexts.
   */
  @Override
  public Credential getCachedActiveCredential() {
    synchronized (loginState) {
      if (loginState.isLoggedIn()) {
        return loginState.getCredential();
      }
      return null;
    }
  }

  /**
   * Clears all credentials. ("logging out" from user perspective.)
   *
   * Safe to call from non-UI contexts.
   */
  @Override
  public void clearCredential() {
    synchronized (loginState) {
      loginState.logOut(false /* Don't prompt for logout. */);
    }
  }

  private static final Logger logger = Logger.getLogger(GoogleLoginService.class.getName());

  private static class LoginServiceLogger implements LoggerFacade {

    @Override
    public void logError(String message, Throwable thrown) {
      logger.log(Level.SEVERE, message, thrown);
    }

    @Override
    public void logWarning(String message) {
      logger.log(Level.WARNING, message);
    }
  };
}
