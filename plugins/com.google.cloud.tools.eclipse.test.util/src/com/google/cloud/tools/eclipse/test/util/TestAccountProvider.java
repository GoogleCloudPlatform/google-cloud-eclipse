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

package com.google.cloud.tools.eclipse.test.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.util.Preconditions;
import com.google.cloud.tools.eclipse.googleapis.Account;
import com.google.cloud.tools.eclipse.googleapis.internal.AccountProvider;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;


/**
 * Test account provider
 */
public class TestAccountProvider extends AccountProvider {

  public static final String EMAIL_ACCOUNT_1 = "test-email-1@mail.com";
  public static final String EMAIL_ACCOUNT_2 = "test-email-2@mail.com";
  public static final String NAME_ACCOUNT_1 = "name-1";
  public static final String NAME_ACCOUNT_2 = "name-2";
  public static final Credential CREDENTIAL_ACCOUNT_1 = new GoogleCredential.Builder().build();
  public static final Credential CREDENTIAL_ACCOUNT_2 = new GoogleCredential.Builder().build();
  public static final String AVATAR_URL_ACCOUNT_1 = "https://avatar.url/account1";
  public static final String AVATAR_URL_ACCOUNT_2 = "https://avatar.url/account2";
  
  public enum State {
    NOT_LOGGED_IN,
    LOGGED_IN,
    LOGGED_IN_SECOND_ACCOUNT
  }
  
  private static Map<State, Optional<Account>> accounts = new EnumMap<>(State.class);
  private static State state = State.LOGGED_IN;
  
  
  public static Account ACCOUNT_1;
  public static Account ACCOUNT_2;
  
  public static final TestAccountProvider INSTANCE = new TestAccountProvider();
  
  private TestAccountProvider() {
    ACCOUNT_1 = new Account(EMAIL_ACCOUNT_1, CREDENTIAL_ACCOUNT_1, NAME_ACCOUNT_1, AVATAR_URL_ACCOUNT_1);
    ACCOUNT_2 = new Account(EMAIL_ACCOUNT_2, CREDENTIAL_ACCOUNT_2, NAME_ACCOUNT_2, AVATAR_URL_ACCOUNT_2);
    accounts.put(State.NOT_LOGGED_IN, Optional.empty());
    accounts.put(State.LOGGED_IN, Optional.of(ACCOUNT_1));
    accounts.put(State.LOGGED_IN_SECOND_ACCOUNT, Optional.of(ACCOUNT_2));
  }
  
  public static void setAsDefaultProvider() {
    GoogleApiFactory.setAccountProvider(INSTANCE);
  }
  
  public static void setAsDefaultProvider(State state) {
    setProviderState(state);
    setAsDefaultProvider();
  }
  
  public static void setProviderState(State state) {
    Preconditions.checkNotNull(state);
    if (TestAccountProvider.state != state) {
      TestAccountProvider.state = state;
      INSTANCE.propagateCredentialChange();
    }
  }
  
  @Override
  public Optional<Account> getAccount() {
    return accounts.get(state);
  }

  @Override
  public Optional<Credential> getCredential() {
    return getAccount().map(Account::getOAuth2Credential);
  }
  
}
