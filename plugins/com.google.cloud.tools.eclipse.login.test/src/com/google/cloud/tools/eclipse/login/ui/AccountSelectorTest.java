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

package com.google.cloud.tools.eclipse.login.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.googleapis.Account;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.util.Optional;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class AccountSelectorTest {

  @Rule public ShellTestResource shellTestResource = new ShellTestResource();
  private Shell shell;
  @Mock private Account account1;
  @Mock private Account account2;
  @Mock private Account account3;
  @Mock private Credential credential1;
  @Mock private Credential credential2;
  @Mock private Credential credential3;
  @Mock private GoogleApiFactory apiFactory;

  @Before
  public void setUp() {
    shell = shellTestResource.getShell();
    GoogleApiFactory.setInstance(apiFactory);
    when(account1.getEmail()).thenReturn("some-email-1@example.com");
    when(account1.getOAuth2Credential()).thenReturn(credential1);
    when(account2.getEmail()).thenReturn("some-email-2@example.com");
    when(account2.getOAuth2Credential()).thenReturn(credential2);
    when(account3.getEmail()).thenReturn("some-email-3@example.com");
    when(account3.getOAuth2Credential()).thenReturn(credential3);
    setAccount(null);
  }
  
  private void setAccount(Account account) {
    if (account != null) {
      when(apiFactory.getAccount()).thenReturn(Optional.of(account));
      when(apiFactory.getCredential()).thenReturn(Optional.of(account.getOAuth2Credential()));
    } else {
      when(apiFactory.getAccount()).thenReturn(Optional.empty());
      when(apiFactory.getCredential()).thenReturn(Optional.empty());
    }
  }
 
  @Test
  public void testComboSetup_noAccount() {
    AccountSelector selector = new AccountSelector(shell);
    assertEquals(0, selector.getAccountCount());
    assertNull(selector.getSelectedCredential());
    assertTrue(selector.getSelectedEmail().isEmpty());
    assertFalse(selector.isEmailAvailable("some-email-1@example.com"));
    assertFalse(selector.isEmailAvailable("some-email-2@example.com"));
    assertFalse(selector.isEmailAvailable("some-email-3@example.com"));
    assertFalse(selector.isSignedIn());
  }

  @Test
  public void testComboSetup_oneAccount() {
    setAccount(account1);
    
    AccountSelector selector = new AccountSelector(shell);
    assertNotNull(selector.getSelectedCredential());
    assertFalse(selector.getSelectedEmail().isEmpty());
    assertEquals(1, selector.getAccountCount());
    assertEquals("some-email-1@example.com", selector.getSelectedEmail());
    assertTrue(selector.isEmailAvailable("some-email-1@example.com"));
    assertTrue(selector.isSignedIn());
    
    setAccount(null);
    
    assertEquals(0, selector.getAccountCount());
    assertNull(selector.getSelectedCredential());
    assertTrue(selector.getSelectedEmail().isEmpty());
    assertFalse(selector.isEmailAvailable("some-email-1@example.com"));
    assertFalse(selector.isEmailAvailable("some-email-2@example.com"));
    assertFalse(selector.isEmailAvailable("some-email-3@example.com"));
    assertFalse(selector.isSignedIn());
  }


  @Test
  public void testIsEmailAvailable_noAccount() {
    AccountSelector selector = new AccountSelector(shell);
    assertFalse(selector.isEmailAvailable(null));
    assertFalse(selector.isEmailAvailable(""));
  }

  @Test
  public void testGetSelectedCredential() {
    AccountSelector selector = new AccountSelector(shell);

    assertTrue(selector.getSelectedEmail().isEmpty());
    assertNull(selector.getSelectedCredential());
    
    setAccount(account1);
    
    assertEquals("some-email-2@example.com", selector.getSelectedEmail());
    assertEquals(credential2, selector.getSelectedCredential());
    
    setAccount(null);
    
    assertTrue(selector.getSelectedEmail().isEmpty());
    assertNull(selector.getSelectedCredential());
  }

  @Test
  public void testIsSignedIn_notSignedIn() {
    AccountSelector selector = new AccountSelector(shell);
    assertFalse(selector.isSignedIn());
  }

  @Test
  public void testIsSignedIn_signedIn() {
    setAccount(account1);
    AccountSelector selector = new AccountSelector(shell);
    assertTrue(selector.isSignedIn());
  }
}
