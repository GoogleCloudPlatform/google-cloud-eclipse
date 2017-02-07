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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.cloud.tools.login.Account;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AccountsPanelTest {

  @Rule public ShellTestResource shellTestResource = new ShellTestResource();
  private Shell shell;

  @Mock private IGoogleLoginService loginService;
  @Mock private Account account1;
  @Mock private Account account2;
  @Mock private Account account3;

  @Before
  public void setUp() {
    shell = shellTestResource.getShell();
    when(account1.getEmail()).thenReturn("alice@example.com");
    when(account2.getEmail()).thenReturn("bob@example.com");
    when(account3.getEmail()).thenReturn("charlie@example.com");
    when(account1.getName()).thenReturn("Alice");
    when(account2.getName()).thenReturn(null);
    when(account3.getName()).thenReturn("Charlie");
  }

  @Test
  public void testLogOutButton_notLoggedIn() {
    setUpLoginService();

    AccountsPanel panel = new AccountsPanel(null, loginService);
    panel.createDialogArea(shell);

    assertNull(panel.logOutButton);
  }

  @Test
  public void testLogOutButton_loggedIn() {
    setUpLoginService(Arrays.asList(account1));

    AccountsPanel panel = new AccountsPanel(null, loginService);
    panel.createDialogArea(shell);

    assertNotNull(panel.logOutButton);
  }

  @Test
  public void testAccountsArea_zeroAccounts() {
    setUpLoginService();

    AccountsPanel panel = new AccountsPanel(null, loginService);
    panel.createDialogArea(shell);

    assertTrue(panel.emailLabels.isEmpty());
  }

  @Test
  public void testAccountsArea_oneAccount() {
    setUpLoginService(Arrays.asList(account1));

    AccountsPanel panel = new AccountsPanel(null, loginService);
    panel.createDialogArea(shell);

    assertEquals(1, panel.emailLabels.size());
    assertEquals("alice@example.com", panel.emailLabels.get(0).getText());
    assertEquals("Alice", panel.nameLabels.get(0).getText());
  }

  @Test
  public void testAccountsArea_accountWithNullName() {
    setUpLoginService(Arrays.asList(account2));

    AccountsPanel panel = new AccountsPanel(null, loginService);
    panel.createDialogArea(shell);

    assertEquals(1, panel.emailLabels.size());
    assertEquals("bob@example.com", panel.emailLabels.get(0).getText());
    assertTrue(panel.nameLabels.get(0).getText().isEmpty());
  }

  @Test
  public void testAccountsArea_threeAccounts() {
    setUpLoginService(Arrays.asList(account1, account2, account3));

    AccountsPanel panel = new AccountsPanel(null, loginService);
    panel.createDialogArea(shell);

    assertEquals(3, panel.emailLabels.size());
    verifyLabelsContains(panel.emailLabels, "alice@example.com");
    verifyLabelsContains(panel.emailLabels, "bob@example.com");
    verifyLabelsContains(panel.emailLabels, "charlie@example.com");
    verifyLabelsContains(panel.nameLabels, "Alice");
    verifyLabelsContains(panel.nameLabels, "");
    verifyLabelsContains(panel.nameLabels, "Charlie");
  }

  private void verifyLabelsContains(List<Label> labels, String text) {
    for (Label label : labels) {
      if (label.getText().equals(text)) {
        return;
      }
    }
    fail();
  }

  private void setUpLoginService(List<Account> accounts) {
    when(loginService.hasAccounts()).thenReturn(!accounts.isEmpty());
    when(loginService.getAccounts()).thenReturn(new HashSet<>(accounts));
  }

  private void setUpLoginService() {
    setUpLoginService(new ArrayList<Account>());  // Simulate no signed-in account.
  }
}
