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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;

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

  // TODO(chanseok): this will be needed later.
  public static final List<String> OAUTH_SCOPES = Collections.unmodifiableList(Arrays.asList(
      "https://www.googleapis.com/auth/userinfo#email"
  ));

  /**
   * Returns the credential of the active user. If there is no active user, returns {@code null}.
   */
  // Should probably be synchronized properly.
  // TODO(chanseok): consider returning a String JSON (i.e., hide Credential)
  public Credential getActiveCredential() {
    return null;
  }
}
