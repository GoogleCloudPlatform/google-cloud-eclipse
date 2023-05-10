/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.googleapis.internal;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.googleapis.Account;
import java.util.Optional;
import org.eclipse.core.runtime.ListenerList;

/**
 * 
 */
public abstract class AccountProvider {

  public abstract Optional<Account> getAccount();
  public abstract Optional<Credential> getCredential();
  protected abstract ListenerList<Runnable> getListeners();
  
  protected void addCredentialChangeListener(Runnable listener) {
    getListeners().add(listener);
  }

  protected void removeCredentialChangeListener(Runnable listener) {
    getListeners().remove(listener);
  }
  
}
