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

import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;

/**
 * Listens for title changes of HTML pages for {@link GoogleLoginBrowser} to capture an authorization
 * code. (The authorization code after successful Google login will be given in the HTML title.)
 */
class AuthorizationCodeListener implements TitleListener {

  private static final String SUCCESS_CODE_PREFIX = "Success code="; //$NON-NLS-1$

  private GoogleLoginBrowser loginBrowser;

  AuthorizationCodeListener(GoogleLoginBrowser loginBrowser) {
    this.loginBrowser = loginBrowser;
  }

  @Override
  public void changed(TitleEvent event) {
    if (event.title != null && event.title.startsWith(SUCCESS_CODE_PREFIX)) {
      loginBrowser.setAuthorizationCode(event.title.substring(SUCCESS_CODE_PREFIX.length()));
      // We don't close the browser now; rather we make the browser log out the user first.
      loginBrowser.logOutAndClose();
    }
  }
}
