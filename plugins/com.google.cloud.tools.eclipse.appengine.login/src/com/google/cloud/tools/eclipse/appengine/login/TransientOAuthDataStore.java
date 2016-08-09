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

import com.google.cloud.tools.ide.login.OAuthData;
import com.google.cloud.tools.ide.login.OAuthDataStore;

import org.eclipse.e4.core.contexts.IEclipseContext;

/**
 * Provides a transient store for saving and loading {@link OAuthData} (a user credential).
 */
public class TransientOAuthDataStore implements OAuthDataStore {

  private static final String STASH_OAUTH_CRED_KEY = "OAUTH_CRED";

  private IEclipseContext eclipseContext;

  public TransientOAuthDataStore(IEclipseContext eclipseContext) {
    this.eclipseContext = eclipseContext;
  }

  @Override
  public void clearStoredOAuthData() {
    eclipseContext.remove(STASH_OAUTH_CRED_KEY);
  }

  @Override
  public OAuthData loadOAuthData() {
    OAuthData credential = (OAuthData) eclipseContext.get(STASH_OAUTH_CRED_KEY);
    if (credential == null) {
      return new OAuthData(null, null, null, null, 0);  // null credential
    }
    return credential;
  }

  @Override
  public void saveOAuthData(OAuthData credential) {
    eclipseContext.set(STASH_OAUTH_CRED_KEY, credential);
  }
}
