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
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.googleapis.internal.IAccountProvider;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Test account provider
 */
public class TestAccountProvider implements IAccountProvider {

  public static final TestAccountProvider INSTANCE = new TestAccountProvider();
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
  
  private static Map<State, Account> accounts = new HashMap<>();
  private static State state = State.LOGGED_IN;
  
  private static Account account1;
  private static Account account2;
  
  private TestAccountProvider() {
    account1 = new Account(EMAIL_ACCOUNT_1, CREDENTIAL_ACCOUNT_1, NAME_ACCOUNT_1, null);
    account2 = new Account(EMAIL_ACCOUNT_2, CREDENTIAL_ACCOUNT_2, NAME_ACCOUNT_2, null);
    accounts.put(State.NOT_LOGGED_IN, null);
    accounts.put(State.LOGGED_IN, account1);
    accounts.put(State.LOGGED_IN_SECOND_ACCOUNT, account2);
  }
  
  public static void setAsDefaultProvider() {
    GoogleApiFactory.setAccountProvider(INSTANCE);
  }
  
  public static void setProviderState(State state) {
    Preconditions.checkNotNull(state);
    TestAccountProvider.state = state;
  }
  
  @Override
  public Account getAccount() throws IOException {
    return accounts.get(state);
  }

  @Override
  public Credential getCredential() throws IOException {
    return hasCredentialsSet() ? getAccount().getOAuth2Credential() : null;
  }

  @Override
  public boolean hasCredentialsSet() {
    return state != State.NOT_LOGGED_IN;
  }
  
}
