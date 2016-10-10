/*******************************************************************************
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
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.login;

import com.google.cloud.tools.ide.login.Account;

import java.util.Set;

/**
 * Provides services around managing Google accounts, including adding new accounts, returning
 * signed-in accounts, signing out, etc.
 */
public interface IGoogleLoginService {

  /**
   * Initiates user login by launching an external browser that a user will interact with
   * to log in.
   *
   * Must be called from a UI context.
   *
   * @param dialogMessage custom login dialog message. Can be {@code null}
   * @return signed-in {@link Account} for successful login; {@code null} otherwise,
   *     including failed and canceled login
   */
  Account logIn(String dialogMessage);

  /**
   * Clears all accounts. ("Logging out" from users' perspective.)
   *
   * Safe to call from non-UI contexts.
   */
  void logOutAll();

  /**
   * @return true if there is at least one signed-in account; false otherwise
   */
  boolean hasAccounts();

  /**
   * Returns currently logged-in accounts.
   *
   * Safe to call from non-UI contexts.
   *
   * @return never {@code null}
   */
  Set<Account> getAccounts();
}
