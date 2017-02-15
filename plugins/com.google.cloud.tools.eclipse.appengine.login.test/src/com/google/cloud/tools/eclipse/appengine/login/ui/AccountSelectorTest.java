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

package com.google.cloud.tools.eclipse.appengine.login.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.login.IGoogleLoginService;
import com.google.cloud.tools.ide.login.Account;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashSet;

@RunWith(MockitoJUnitRunner.class)
public class AccountSelectorTest {

  @Rule public ShellTestResource shellTestResource = new ShellTestResource();
  private Shell shell;

  @Mock private IGoogleLoginService loginService;
  @Mock private Account account1;
  @Mock private Account account2;
  @Mock private Account account3;
  @Mock private Credential credential1;
  @Mock private Credential credential2;
  @Mock private Credential credential3;

  @Before
  public void setUp() {
    shell = shellTestResource.getShell();
    when(account1.getEmail()).thenReturn("some-email-1@example.com");
    when(account1.getOAuth2Credential()).thenReturn(credential1);
    when(account2.getEmail()).thenReturn("some-email-2@example.com");
    when(account2.getOAuth2Credential()).thenReturn(credential2);
    when(account3.getEmail()).thenReturn("some-email-3@example.com");
    when(account3.getOAuth2Credential()).thenReturn(credential3);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructor_nullLoginMessage() {
    new AccountSelector(shell, loginService, null);
  }

  @Test
  public void testComboSetup_noAccount() {
    when(loginService.getAccounts()).thenReturn(new HashSet<Account>());

    AccountSelector selector = new AccountSelector(shell, loginService, "<select this to login>");
    assertEquals(-1, selector.combo.getSelectionIndex());
    assertNull(selector.getSelectedCredential());
    assertTrue(selector.getSelectedEmail().isEmpty());
    assertEquals(1, selector.combo.getItemCount());
    assertEquals("<select this to login>", selector.combo.getItem(0));
  }

  @Test
  public void testComboSetup_oneAccount() {
    when(loginService.getAccounts()).thenReturn(new HashSet<>(Arrays.asList(account1)));

    AccountSelector selector = new AccountSelector(shell, loginService, "<select this to login>");
    assertEquals(-1, selector.combo.getSelectionIndex());
    assertNull(selector.getSelectedCredential());
    assertTrue(selector.getSelectedEmail().isEmpty());
    assertEquals(2, selector.combo.getItemCount());
    assertEquals("some-email-1@example.com", selector.combo.getItem(0));
    assertEquals("<select this to login>", selector.combo.getItem(1));
  }

  @Test
  public void testComboSetup_threeAccounts() {
    when(loginService.getAccounts())
        .thenReturn(new HashSet<>(Arrays.asList(account1, account2, account3)));

    AccountSelector selector = new AccountSelector(shell, loginService, "<select this to login>");
    assertEquals(-1, selector.combo.getSelectionIndex());
    assertNull(selector.getSelectedCredential());
    assertTrue(selector.getSelectedEmail().isEmpty());
    assertEquals(4, selector.combo.getItemCount());
    String allEmails =
        selector.combo.getItem(0) + selector.combo.getItem(1) + selector.combo.getItem(2);
    assertTrue(allEmails.contains("some-email-1@example.com"));
    assertTrue(allEmails.contains("some-email-2@example.com"));
    assertTrue(allEmails.contains("some-email-3@example.com"));
    assertEquals("<select this to login>", selector.combo.getItem(3));
  }

  @Test
  public void testIsEmailAvailable_noAccount() {
    when(loginService.getAccounts()).thenReturn(new HashSet<Account>());

    AccountSelector selector = new AccountSelector(shell, loginService, "<select this to login>");
    assertFalse(selector.isEmailAvailable(null));
    assertFalse(selector.isEmailAvailable(""));
  }

  @Test
  public void testIsEmailAvailable_threeAccounts() {
    when(loginService.getAccounts())
        .thenReturn(new HashSet<>(Arrays.asList(account1, account2, account3)));

    AccountSelector selector = new AccountSelector(shell, loginService, "<select this to login>");
    assertFalse(selector.isEmailAvailable(null));
    assertFalse(selector.isEmailAvailable(""));
    assertTrue(selector.isEmailAvailable("some-email-1@example.com"));
    assertTrue(selector.isEmailAvailable("some-email-2@example.com"));
    assertTrue(selector.isEmailAvailable("some-email-3@example.com"));
  }

  @Test
  public void testGetSelectedCredential() {
    when(loginService.getAccounts())
        .thenReturn(new HashSet<>(Arrays.asList(account1, account2, account3)));
    AccountSelector selector = new AccountSelector(shell, loginService, "<select this to login>");

    assertEquals(-1, selector.combo.getSelectionIndex());
    assertNull(selector.getSelectedCredential());

    int index = selector.combo.indexOf("some-email-2@example.com");
    simulateSelect(selector, index);

    assertEquals(index, selector.combo.getSelectionIndex());
    assertEquals("some-email-2@example.com", selector.getSelectedEmail());
    assertEquals("some-email-2@example.com", selector.combo.getItem(index));
    assertEquals(credential2, selector.getSelectedCredential());
  }

  @Test
  public void testSelectAccount() {
    when(loginService.getAccounts())
        .thenReturn(new HashSet<>(Arrays.asList(account1, account2, account3)));
    AccountSelector selector = new AccountSelector(shell, loginService, "<select this to login>");

    assertEquals(-1, selector.combo.getSelectionIndex());
    assertTrue(selector.getSelectedEmail().isEmpty());
    assertNull(selector.getSelectedCredential());

    selector.selectAccount("some-email-2@example.com");

    assertNotEquals(-1, selector.combo.getSelectionIndex());
    assertEquals("some-email-2@example.com", selector.getSelectedEmail());
    assertEquals(credential2, selector.getSelectedCredential());
  }

  @Test
  public void testSelectAccount_nonExistingEmail() {
    when(loginService.getAccounts()).thenReturn(new HashSet<>(Arrays.asList(account1, account2)));
    AccountSelector selector = new AccountSelector(shell, loginService, "<select this to login>");

    assertEquals(-1, selector.combo.getSelectionIndex());
    assertTrue(selector.getSelectedEmail().isEmpty());

    selector.selectAccount("non-existing-email@example.com");

    assertEquals(-1, selector.combo.getSelectionIndex());
    assertTrue(selector.getSelectedEmail().isEmpty());
  }

  @Test
  public void testSelectAccount_nullOrEmptyEmail() {
    when(loginService.getAccounts()).thenReturn(new HashSet<>(Arrays.asList(account1, account2)));
    AccountSelector selector = new AccountSelector(shell, loginService, "<select this to login>");

    assertEquals(-1, selector.combo.getSelectionIndex());
    assertTrue(selector.getSelectedEmail().isEmpty());

    selector.selectAccount(null);

    assertEquals(-1, selector.combo.getSelectionIndex());
    assertTrue(selector.getSelectedEmail().isEmpty());

    selector.selectAccount("");

    assertEquals(-1, selector.combo.getSelectionIndex());
    assertTrue(selector.getSelectedEmail().isEmpty());
  }

  @Test
  public void testLogin_itemAddedAtTopAndSelected() {
    when(loginService.getAccounts()).thenReturn(new HashSet<>(Arrays.asList(account1, account2)));
    when(loginService.logIn(anyString())).thenReturn(account3);
    AccountSelector selector = new AccountSelector(shell, loginService, "<select this to login>");
    assertEquals(3, selector.combo.getItemCount());

    assertEquals("<select this to login>", selector.combo.getItem(2));
    simulateSelect(selector, 2);

    assertEquals(4, selector.combo.getItemCount());
    assertEquals("some-email-3@example.com", selector.getSelectedEmail());
    assertEquals("some-email-3@example.com", selector.combo.getItem(0));
    assertEquals(0, selector.combo.getSelectionIndex());
    assertEquals(credential3, selector.getSelectedCredential());
    assertEquals("<select this to login>", selector.combo.getItem(3));
  }

  @Test
  public void testLogin_existingEmail() {
    when(loginService.getAccounts())
        .thenReturn(new HashSet<>(Arrays.asList(account1, account2, account3)));
    when(loginService.logIn(anyString())).thenReturn(account1);
    AccountSelector selector = new AccountSelector(shell, loginService, "<select this to login>");
    assertEquals(4, selector.combo.getItemCount());
    assertEquals(-1, selector.combo.getSelectionIndex());

    assertEquals("<select this to login>", selector.combo.getItem(3));
    simulateSelect(selector, 3);

    assertEquals(4, selector.combo.getItemCount());
    assertNotEquals(-1, selector.combo.getSelectionIndex());
    assertEquals("some-email-1@example.com", selector.getSelectedEmail());
    assertEquals("some-email-1@example.com", selector.combo.getText());
    assertEquals(credential1, selector.getSelectedCredential());
  }

  @Test
  public void testFailedLogin_deselectLoginLinkItem() {
    when(loginService.getAccounts()).thenReturn(new HashSet<>(Arrays.asList(account1, account2)));
    when(loginService.logIn(anyString())).thenReturn(null);
    AccountSelector selector = new AccountSelector(shell, loginService, "<select this to login>");
    assertEquals(3, selector.combo.getItemCount());

    simulateSelect(selector, 1);
    assertEquals(1, selector.combo.getSelectionIndex());
    assertNotNull(selector.getSelectedCredential());
    assertFalse(selector.getSelectedEmail().isEmpty());

    assertEquals("<select this to login>", selector.combo.getItem(2));
    simulateSelect(selector, 2);

    assertEquals(3, selector.combo.getItemCount());
    assertEquals(-1, selector.combo.getSelectionIndex());
    assertNull(selector.getSelectedCredential());
    assertTrue(selector.getSelectedEmail().isEmpty());
    assertEquals("<select this to login>", selector.combo.getItem(2));
  }

  private void simulateSelect(AccountSelector selector, int index) {
    selector.combo.select(index);
    selector.combo.notifyListeners(SWT.Selection, mock(Event.class));
  }
}
