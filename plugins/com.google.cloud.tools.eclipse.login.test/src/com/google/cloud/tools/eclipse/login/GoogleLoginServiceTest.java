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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.login.ui.LoginServiceUi;
import com.google.cloud.tools.login.Account;
import com.google.cloud.tools.login.GoogleLoginState;
import com.google.cloud.tools.login.LoggerFacade;
import com.google.cloud.tools.login.OAuthData;
import com.google.cloud.tools.login.OAuthDataStore;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GoogleLoginServiceTest {

  @Mock private GoogleLoginState loginState;
  @Mock private OAuthDataStore dataStore;
  @Mock private OAuthData savedOAuthData;
  @Mock private LoginServiceUi uiFacade;
  @Mock private LoggerFacade loggerFacade;

  @Mock private Account account1;
  @Mock private Account account2;
  @Mock private Account account3;

  private static final Set<String> OAUTH_SCOPES = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(
          "email",
          "https://www.googleapis.com/auth/cloud-platform"
      )));

  @Before
  public void setUp() throws IOException {
    when(account1.getEmail()).thenReturn("some-email-1@example.com");
    when(account2.getEmail()).thenReturn("some-email-2@example.com");
    when(account3.getEmail()).thenReturn("some-email-3@example.com");

    Set<OAuthData> oAuthDataSet = new HashSet<>(Arrays.asList(savedOAuthData));
    when(dataStore.loadOAuthData()).thenReturn(oAuthDataSet);
  }

  @Test
  public void testIsLoggedIn() {
    GoogleLoginService loginService = new GoogleLoginService(dataStore, uiFacade, loggerFacade);
    assertFalse(loginService.hasAccounts());
  }

  @Test
  public void testGetAccount() {
    GoogleLoginService loginService = new GoogleLoginService(dataStore, uiFacade, loggerFacade);
    assertTrue(loginService.getAccounts().isEmpty());
  }

  @Test
  public void testLogIn_successfulLogin() {
    GoogleLoginService loginService = newLoginServiceWithMockLoginState(true /* set up logins */);
    Account account = loginService.logIn();

    assertEquals(account1, account);
    assertTrue(loginService.hasAccounts());
    // Comparison between accounts is conveniently based only on email. (See 'Account.equals().')
    assertEquals(1, loginService.getAccounts().size());
    assertEquals(account1, loginService.getAccounts().iterator().next());
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
    assertTrue(accounts1.contains(account1));

    loginService.logIn();
    Set<Account> accounts2 = loginService.getAccounts();
    assertEquals(2, accounts2.size());
    assertTrue(accounts2.contains(account1));
    assertTrue(accounts2.contains(account2));

    loginService.logIn();
    Set<Account> accounts3 = loginService.getAccounts();
    assertEquals(3, accounts3.size());
    assertTrue(accounts3.contains(account1));
    assertTrue(accounts3.contains(account2));
    assertTrue(accounts3.contains(account3));
  }

  @Test
  public void testLogOutAll() {
    GoogleLoginService loginService = newLoginServiceWithMockLoginState(true);

    loginService.logIn();
    loginService.logIn();
    loginService.logIn();

    assertTrue(loginService.hasAccounts());
    assertFalse(loginService.getAccounts().isEmpty());

    loginService.logOutAll();

    assertFalse(loginService.hasAccounts());
    assertTrue(loginService.getAccounts().isEmpty());
  }

  @Test
  public void testGetCredential_nullEmail() {
    try {
      new GoogleLoginService(loginState).getCredential(null);
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

    Credential credential = loginService.getCredential("some-email-1@example.com");
    assertEquals(account1.getOAuth2Credential(), credential);
  }

  @Test
  public void testGetCredential_emailNotLoggedIn() {
    GoogleLoginService loginService = newLoginServiceWithMockLoginState(true);
    loginService.logIn();
    assertEquals(1, loginService.getAccounts().size());

    assertNull(loginService.getCredential("non-existing@example.com"));
  }

  @Test
  public void testGoogleLoginService_removeSavedCredentialIfNullRefreshToken() 
      throws IOException {
    when(savedOAuthData.getEmail()).thenReturn("my-email@example.com");
    when(savedOAuthData.getStoredScopes()).thenReturn(OAUTH_SCOPES);
    when(savedOAuthData.getRefreshToken()).thenReturn(null);

    new GoogleLoginService(dataStore, uiFacade, loggerFacade);
    verify(dataStore).removeOAuthData("my-email@example.com");
  }

  @Test
  public void testGoogleLoginService_removeSavedCredentialIfScopesChanged()
      throws IOException {
    // Credential in the data store has an out-dated scopes.
    Set<String> newScope = new HashSet<>(Arrays.asList("new_scope"));
    when(savedOAuthData.getEmail()).thenReturn("my-email@example.com");
    when(savedOAuthData.getStoredScopes()).thenReturn(newScope);
    when(savedOAuthData.getRefreshToken()).thenReturn("fake_refresh_token");

    new GoogleLoginService(dataStore, uiFacade, loggerFacade);
    verify(dataStore).removeOAuthData("my-email@example.com");
  }

  @Test
  public void testGoogleLoginService_restoreSavedCredential()
      throws IOException {
    // Credential in the data store is valid.
    when(savedOAuthData.getEmail()).thenReturn("my-email@example.com");
    when(savedOAuthData.getStoredScopes()).thenReturn(OAUTH_SCOPES);
    when(savedOAuthData.getRefreshToken()).thenReturn("fake_refresh_token");

    new GoogleLoginService(dataStore, uiFacade, loggerFacade);
    verify(dataStore, never()).removeOAuthData("my-email@example.com");
    verify(dataStore, never()).clearStoredOAuthData();
  }

  @Test
  public void testGetGoogleLoginUrl() {
    String customRedirectUrl = "http://127.0.0.1:12345/Consumer";

    String loginUrl = GoogleLoginService.getGoogleLoginUrl(customRedirectUrl);
    assertTrue(loginUrl.startsWith("https://accounts.google.com/o/oauth2/auth?"));
    assertTrue(loginUrl.contains("redirect_uri=" + customRedirectUrl));
  }

  private GoogleLoginService newLoginServiceWithMockLoginState(boolean setUpSuccessfulLogins) {
    GoogleLoginService loginService = new GoogleLoginService(loginState);

    if (setUpSuccessfulLogins) {
      when(loginState.logInWithLocalServer(anyString()))
          .thenReturn(account1).thenReturn(account2).thenReturn(account3);
      when(loginState.listAccounts())
          .thenReturn(new HashSet<>(Arrays.asList(account1)))
          .thenReturn(new HashSet<>(Arrays.asList(account1, account2)))
          .thenReturn(new HashSet<>(Arrays.asList(account1, account2, account3)));
    } else {
      when(loginState.logInWithLocalServer(anyString())).thenReturn(null);
      when(loginState.listAccounts()).thenReturn(new HashSet<Account>());
    }

    return loginService;
  }
}
