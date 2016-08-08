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

public interface IGoogleLoginService {

  /**
   * Returns the credential of an active user (among multiple logged-in users). A login screen
   * may be presented, e.g., if no user is logged in or login is required due to an expired
   * credential. This method returns {@code null} if a user cancels the login process.
   * For this reason, if {@code null} is returned, the caller should cancel the current
   * operation and display a general message that login is required but was cancelled or failed.
   *
   * Must be called from a UI context.
   */
  public Credential getActiveCredential();

  /**
   * Returns the credential of an active user (among multiple logged-in users). Unlike {@link
   * #getActiveCredential}, this version does not involve login process or make API calls.
   * Returns {@code null} if no credential has been cached.
   *
   * Safe to call from non-UI contexts.
   */
  public Credential getCachedActiveCredential();

  /**
   * Clears all credentials. ("logging out" from user perspective.)
   *
   * Safe to call from non-UI contexts.
   */
  public void clearCredential();
}
