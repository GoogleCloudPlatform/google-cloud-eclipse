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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import com.google.cloud.tools.login.Account;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
    Control control = panel.createDialogArea(shell);

    List<String> buttonTexts = collectButtonTexts((Composite) control);
    assertEquals(1, buttonTexts.size());
    assertEquals("Add Account...", buttonTexts.get(0));
  }

  @Test
  public void testLogOutButton_loggedIn() {
    setUpLoginService(Arrays.asList(account1));

    AccountsPanel panel = new AccountsPanel(null, loginService);
    Control control = panel.createDialogArea(shell);

    List<String> buttonTexts = collectButtonTexts((Composite) control);
    assertEquals(2, buttonTexts.size());
    assertTrue(buttonTexts.contains("Add Account..."));
    assertTrue(buttonTexts.contains("Sign Out..."));
  }

  @Test
  public void testAccountsArea_zeroAccounts() {
    setUpLoginService();

    AccountsPanel panel = new AccountsPanel(null, loginService);
    Control control = panel.createDialogArea(shell);

    NamesEmails namesEmails = collectNamesEmails(control);
    assertTrue(namesEmails.emails.isEmpty());
  }

  @Test
  public void testAccountsArea_oneAccount() {
    setUpLoginService(Arrays.asList(account1));

    AccountsPanel panel = new AccountsPanel(null, loginService);
    Control control = panel.createDialogArea(shell);

    NamesEmails namesEmails = collectNamesEmails(control);
    assertEquals(1, namesEmails.emails.size());
    assertEquals("alice@example.com", namesEmails.emails.get(0));
    assertEquals("Alice", namesEmails.names.get(0));
  }

  @Test
  public void testAccountsArea_accountWithNullName() {
    setUpLoginService(Arrays.asList(account2));

    AccountsPanel panel = new AccountsPanel(null, loginService);
    Control control = panel.createDialogArea(shell);

    NamesEmails namesEmails = collectNamesEmails(control);
    assertEquals(1, namesEmails.emails.size());
    assertEquals("bob@example.com", namesEmails.emails.get(0));
    assertTrue(namesEmails.names.get(0).isEmpty());
  }

  @Test
  public void testAccountsArea_threeAccounts() {
    setUpLoginService(Arrays.asList(account1, account2, account3));

    AccountsPanel panel = new AccountsPanel(null, loginService);
    Control control = panel.createDialogArea(shell);

    NamesEmails namesEmails = collectNamesEmails(control);
    assertEquals(3, namesEmails.emails.size());
    assertTrue(namesEmails.emails.contains("alice@example.com"));
    assertTrue(namesEmails.emails.contains("bob@example.com"));
    assertTrue(namesEmails.emails.contains("charlie@example.com"));
    assertTrue(namesEmails.names.contains("Alice"));
    assertTrue(namesEmails.names.contains(""));
    assertTrue(namesEmails.names.contains("Charlie"));
  }

  private void setUpLoginService(List<Account> accounts) {
    when(loginService.hasAccounts()).thenReturn(!accounts.isEmpty());
    when(loginService.getAccounts()).thenReturn(new HashSet<>(accounts));
  }

  private void setUpLoginService() {
    setUpLoginService(new ArrayList<Account>());  // Simulate no signed-in account.
  }

  private static List<String> collectButtonTexts(Composite composite) {
    List<String> buttonTexts = new ArrayList<>();
    for (Control control : composite.getChildren()) {
      if (control instanceof Button) {
        buttonTexts.add(((Button) control).getText());
      } else if (control instanceof Composite) {
        buttonTexts.addAll(collectButtonTexts((Composite) control));
      }
    }
    return buttonTexts;
  }

  private static class NamesEmails {
    private List<String> names = new ArrayList<>();
    private List<String> emails = new ArrayList<>();
  }

  private static NamesEmails collectNamesEmails(Control dialogArea) {
    NamesEmails namesEmails = new NamesEmails();

    Control controls[] = ((Composite) dialogArea).getChildren();
    for (int i = 0; i + 3 < controls.length; i += 3) {
      namesEmails.names.add(((Label) controls[i]).getText());
      namesEmails.emails.add(((Label) controls[i+1]).getText());
      assertEquals(SWT.SEPARATOR, ((Label) controls[i+2]).getStyle() & SWT.SEPARATOR);
    }
    return namesEmails;
  }
}
