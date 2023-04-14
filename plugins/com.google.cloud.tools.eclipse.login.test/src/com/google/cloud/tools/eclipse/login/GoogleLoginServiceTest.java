/*
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
 */

package com.google.cloud.tools.eclipse.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.googleapis.Account;
import com.google.cloud.tools.eclipse.login.ui.LoginServiceUi;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider.State;
import com.google.cloud.tools.login.LoggerFacade;
import com.google.cloud.tools.login.OAuthData;
import com.google.cloud.tools.login.OAuthDataStore;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GoogleLoginServiceTest {

  @Mock private OAuthDataStore dataStore;
  @Mock private OAuthData savedOAuthData;
  @Mock private LoginServiceUi uiFacade;
  @Mock private LoggerFacade loggerFacade;

  @Mock private Account account1;
  @Mock private Account account2;
  @Mock private Account account3;

  @Before
  public void setUp() throws IOException {
    TestAccountProvider.setAsDefaultProvider(State.NOT_LOGGED_IN);
    when(account1.getEmail()).thenReturn("some-email-1@example.com");
    when(account2.getEmail()).thenReturn("some-email-2@example.com");
    when(account3.getEmail()).thenReturn("some-email-3@example.com");

    Set<OAuthData> oAuthDataSet = new HashSet<>(Arrays.asList(savedOAuthData));
    when(dataStore.loadOAuthData()).thenReturn(oAuthDataSet);
  }

  @Test
  public void testIsLoggedIn() {
    GoogleLoginService loginService = new GoogleLoginService();
    assertFalse(loginService.hasAccounts());
  }

  @Test
  public void testGetAccount() {
    GoogleLoginService loginService = new GoogleLoginService();
    assertTrue(loginService.getAccounts().isEmpty());
  }

  @Test
  public void testLogIn_successfulLogin() {
    GoogleLoginService loginService = newLoginServiceWithMockLoginState(true /* set up logins */);
    Account account = loginService.logIn();

    assertEquals(TestAccountProvider.ACCOUNT_1, account);
    assertTrue(loginService.hasAccounts());
    // Comparison between accounts is conveniently based only on email. (See 'Account.equals().')
    assertEquals(1, loginService.getAccounts().size());
    assertEquals(TestAccountProvider.ACCOUNT_1, loginService.getAccounts().iterator().next());
  }

  @Test
  public void testLogIn_failedLogin() {
    GoogleLoginService loginService = newLoginServiceWithMockLoginState(false /* failed login */);
    Account account = loginService.logIn();

    assertNull(account);
    assertFalse(loginService.hasAccounts());
    assertTrue(loginService.getAccounts().isEmpty());
  }

  @Test
  public void testMultipleLogins() {
    GoogleLoginService loginService = newLoginServiceWithMockLoginState(true);

    loginService.logIn();
    Set<Account> accounts1 = loginService.getAccounts();
    assertEquals(1, accounts1.size());
    assertTrue(accounts1.contains(TestAccountProvider.ACCOUNT_1));

    TestAccountProvider.setProviderState(State.LOGGED_IN_SECOND_ACCOUNT);
    loginService.logIn();
    Set<Account> accounts2 = loginService.getAccounts();
    assertEquals(1, accounts2.size());
    assertTrue(accounts2.contains(TestAccountProvider.ACCOUNT_2));
  }

  @Test
  public void testLogOutAll() {
    GoogleLoginService loginService = newLoginServiceWithMockLoginState(true);

    loginService.logIn();

    assertTrue(loginService.hasAccounts());
    assertFalse(loginService.getAccounts().isEmpty());

    TestAccountProvider.setProviderState(State.NOT_LOGGED_IN);

    assertFalse(loginService.hasAccounts());
    assertTrue(loginService.getAccounts().isEmpty());
  }

  @Test
  public void testGetCredential_nullEmail() {
    try {
      new GoogleLoginService().getCredential(null);
      fail();
    } catch (NullPointerException ex) {
      assertEquals("email cannot be null.", ex.getMessage());
    }
  }

  @Test
  public void testGetCredential() {
    GoogleLoginService loginService = newLoginServiceWithMockLoginState(true);
    loginService.logIn();
    assertEquals(1, loginService.getAccounts().size());

    Credential credential = loginService.getCredential(TestAccountProvider.EMAIL_ACCOUNT_1);
    assertEquals(TestAccountProvider.ACCOUNT_1.getOAuth2Credential(), credential);
  }

  @Test
  public void testGetCredential_emailNotLoggedIn() {
    GoogleLoginService loginService = newLoginServiceWithMockLoginState(true);
    loginService.logIn();
    assertEquals(1, loginService.getAccounts().size());

    assertNull(loginService.getCredential("non-existing@example.com"));
  }


  private GoogleLoginService newLoginServiceWithMockLoginState(boolean setUpSuccessfulLogins) {
    TestAccountProvider.setProviderState(setUpSuccessfulLogins ? State.LOGGED_IN : State.NOT_LOGGED_IN);
    return new GoogleLoginService();
  }
}
