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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.common.annotations.VisibleForTesting;

import java.net.URL;
import java.util.Collection;

/**
 * A login dialog that is specific for Google login. The dialog embeds a full-featured browser,
 * which is used to connect to a Google login URL. Successful browser login will auto-close
 * the dialog (after explicitly logging out the user from the browser), and the authorization
 * code can be retrieved by {@link #getAuthorizationCode()} thereafter.
 *
 * Implementation detail worth noting: successful browser login will return an authorization code
 * as the title of an HTML page (i.e., the redirect URL after login is basically set to
 * {@link GoogleOAuthConstants.OOB_REDIRECT_URI}, which is {@code "urn:ietf:wg:oauth:2.0:oob"}).
 */
public class GoogleLoginBrowser extends Dialog {

  private static final String LOGOUT_URL = "https://www.google.com/accounts/Logout"; //$NON-NLS-1$

  private Browser browser;
  private URL loginUrl;

  private String authorizationCode;

  public GoogleLoginBrowser(Shell parentShell,
      String OAuthClientId, Collection<String> OAuthScopes) {
    super(parentShell);
    loginUrl = new GoogleAuthorizationCodeRequestUrl(
        OAuthClientId, GoogleOAuthConstants.OOB_REDIRECT_URI + ":auto", OAuthScopes).toURL();
  }

  /**
   * @return authorization code or {@code null} if login is not completed for whatever reason.
   */
  public String getAuthorizationCode() {
    return authorizationCode;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Sign in to Google");  // TODO(chanseok): localize after PR #386 lands.
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);

    browser = new Browser(composite, SWT.BORDER);
    browser.setUrl(loginUrl.toString());
    browser.addProgressListener(new PageLoadingListener());
    browser.addTitleListener(new AuthorizationCodeListener(this));
    GridDataFactory.fillDefaults().grab(true, true).hint(1060, 660).applyTo(browser);

    return composite;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  @Override
  protected boolean isResizable() {
    return true;
  }

  /**
   *  To close the login browser after verifying that the browser is in the logged-out state.
   *  Logging out (inside the browser) is triggered via {@link #LOGOUT_URL} right after the
   *  retrieval of an authorization code from successful browser login. (See {@link
   *  AuthorizationCodeListener#changed(TitleEvent)}.) (If we don't make the browser log out
   *  the user, the next time the login browser is launched, the browser may already be in
   *  the logged-in state.)
   */
  private class PageLoadingListener extends ProgressAdapter {
    @Override
    public void completed(ProgressEvent event) {
      if (authorizationCode != null) {
        close();
      }
    }
  }

  @VisibleForTesting
  protected void setAuthorizationCode(String authorizationCode) {
    this.authorizationCode = authorizationCode;
  }

  @VisibleForTesting
  protected void logOutAndClose() {
    browser.setVisible(false);
    browser.setUrl(LOGOUT_URL);
  }
}
