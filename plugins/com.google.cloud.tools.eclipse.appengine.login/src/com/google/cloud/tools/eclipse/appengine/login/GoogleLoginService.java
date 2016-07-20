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
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.util.Utils;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provides service related to login, e.g., account management, getting a credential of a
 * currently active user, etc.
 *
 * Thread-safe.
 */
public class GoogleLoginService {

  // TODO(chanseok): these constant values should be set at compile-time to hide actual values.
  // For this purpose, we could use org.codehaus.mojo:templating-maven-plugin as in the
  // .eclipse.usagetracker bundle.
  public static final String OAUTH_CLIENT_ID = "@oauth.client.id@";
  public static final String OAUTH_CLIENT_SECRET = "@oauth.client.secret@";

  private static final List<String> OAUTH_SCOPES = Collections.unmodifiableList(Arrays.asList(
      "email",
      "https://www.googleapis.com/auth/cloud-platform"
  ));

  /**
   * Returns the credential of the active user. If there is no active user, returns {@code null}.
   */
  // Should probably be synchronized properly.
  public Credential getActiveCredential(IShellProvider shellProvider) throws IOException {
    return logIn(shellProvider);
  }

  private Credential logIn(IShellProvider shellProvider) throws IOException {
    GoogleAuthorizationCodeRequestUrl requestUrl =
        new GoogleAuthorizationCodeRequestUrl(OAUTH_CLIENT_ID,
                                              GoogleOAuthConstants.OOB_REDIRECT_URI,
                                              OAUTH_SCOPES);

    try {
      IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
      browserSupport.getExternalBrowser().openURL(requestUrl.toURL());
    } catch (PartInitException pie) {  // requires org.eclipse.equinox.common OSGi bundle.
      MessageDialog.openError(shellProvider.getShell(),
          "Error launching external browser", pie.getMessage());
      return null;
    }

    InputDialog dialog = new InputDialog(shellProvider.getShell(),
        "Enter Verification Code", "Enter verification code from the browser login.", null, null);
    if (dialog.open() != InputDialog.OK) {
      return null;
    }

    String verificationCode = dialog.getValue();
    GoogleAuthorizationCodeTokenRequest authRequest = new GoogleAuthorizationCodeTokenRequest(
        Utils.getDefaultTransport(), Utils.getDefaultJsonFactory(),
        OAUTH_CLIENT_ID, OAUTH_CLIENT_SECRET,
        verificationCode,
        GoogleOAuthConstants.OOB_REDIRECT_URI);

    return createCredential(authRequest.execute());
  }

  private Credential createCredential(GoogleTokenResponse tokenResponse) {
    GoogleCredential credential = new GoogleCredential.Builder()
        .setTransport(Utils.getDefaultTransport())
        .setJsonFactory(Utils.getDefaultJsonFactory())
        .setClientSecrets(OAUTH_CLIENT_ID, OAUTH_CLIENT_SECRET)
        .build();
    credential.setAccessToken(tokenResponse.getAccessToken());
    credential.setRefreshToken(tokenResponse.getRefreshToken());
    return credential;
  }
}
