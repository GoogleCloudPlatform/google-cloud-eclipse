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

package com.google.cloud.tools.eclipse.login;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.cloud.tools.eclipse.googleapis.Account;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.login.ui.LoginServiceUi;
import com.google.cloud.tools.login.JavaPreferenceOAuthDataStore;
import com.google.cloud.tools.login.LoggerFacade;
import com.google.cloud.tools.login.OAuthDataStore;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Provides service related to login, e.g., account management, getting a credential, etc.
 */
public class GoogleLoginService implements IGoogleLoginService {
  
  private static final String PREFERENCE_PATH_OAUTH_DATA_STORE =
      "/com/google/cloud/tools/eclipse/login/datastore";

  // For the detailed info about each scope, see
  // https://github.com/GoogleCloudPlatform/gcloud-eclipse-tools/wiki/Cloud-Tools-for-Eclipse-Technical-Design#oauth-20-scopes-requested
  private static final Set<String> OAUTH_SCOPES = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(
          "email", //$NON-NLS-1$
          "https://www.googleapis.com/auth/cloud-platform" //$NON-NLS-1$
      )));

  /**
   * Returns a URL through which users can login.
   *
   * @param redirectUrl URL to which the login result is directed. For example, a local web
   *     server listening on the URL can receive an authorization code from it.
   */
  public static String getGoogleLoginUrl(String redirectUrl) {
    return new GoogleAuthorizationCodeRequestUrl(Constants.getOAuthClientId(), redirectUrl,
        OAUTH_SCOPES).toString();
  }

  private Set<Account> accounts = new HashSet<>();

  /**
   * Called by OSGi Declarative Services Runtime when the {@link GoogleLoginService} is activated
   * as an OSGi service.
   */
  protected void activate() {
    IShellProvider shellProvider = () -> {
      IWorkbench workbench = PlatformUI.getWorkbench();
      IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
      if (window != null && window.getShell() != null) {
        return window.getShell();
      }
      return workbench.getDisplay().getActiveShell();
    };

    LoginServiceLogger loginServiceLogger = new LoginServiceLogger();
    LoginServiceUi uiFacade = new LoginServiceUi(shellProvider);
    OAuthDataStore dataStore =
        new JavaPreferenceOAuthDataStore(PREFERENCE_PATH_OAUTH_DATA_STORE, loginServiceLogger);
    updateAccounts();
  }

  /**
   * 0-arg constructor is necessary for OSGi Declarative Services. Initialization will be done
   * by {@link #activate()}.
   */
  public GoogleLoginService() {}
  
  /**
   * Obtains an unary set of accounts from GoogleAPiFactory
   * @return the found account, or null
   */
  private Account updateAccounts() {
    GoogleApiFactory apiFactory = new GoogleApiFactory();
    accounts = new HashSet<>();
    Account account = null;
    if (apiFactory.hasCredentialsSet()) {
      try {
        account = apiFactory.getAccount();
        accounts.add(account);
      } catch (IOException ex) {
        logger.log(Level.SEVERE, "Exception when obtaining account", ex);
      }
    }
    return account;
  }
  
  @Override
  public Account logIn() {
    // TODO: holding a lock for a long period of time (especially when waiting for UI events)
    // should be avoided. Make the login library thread-safe, and don't lock during UI events.
    // (https://github.com/GoogleCloudPlatform/ide-login/issues/21)
    synchronized (this) {
      return updateAccounts();
    }
  }

  @Override
  public void logOutAll() {
    synchronized (this) {
      accounts = new HashSet<>();
    }
  }

  @Override
  public boolean hasAccounts() {
    synchronized (this) {
      return !accounts.isEmpty();
    }
  }

  @Override
  public Set<Account> getAccounts() {
    synchronized (this) {
      return new HashSet<>(accounts);
    }
  }

  @Override
  public Credential getCredential(String email) {
    Preconditions.checkNotNull(email, "email cannot be null.");
    synchronized (this) {
      for (Account account : accounts) {
        if (account.getEmail().equals(email)) {
          return account.getOAuth2Credential();
        }
      }
    }
    return null;
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
  }
}
