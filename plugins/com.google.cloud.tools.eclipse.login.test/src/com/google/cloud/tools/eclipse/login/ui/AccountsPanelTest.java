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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
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

  @Test(expected = WidgetNotFoundException.class)
  public void testLogOutButton_notLoggedIn() {
    setUpLoginService();

    AccountsPanel panel = new AccountsPanel(null, loginService);
    Control control = panel.createDialogArea(shell);

    new SWTBot(control).buttonWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "logOutButton");
  }

  @Test
  public void testLogOutButton_loggedIn() {
    setUpLoginService(Arrays.asList(account1));

    AccountsPanel panel = new AccountsPanel(null, loginService);
    Control control = panel.createDialogArea(shell);

    SWTBotButton button =
        new SWTBot(control).buttonWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "logOutButton");
    assertEquals("Sign Out...", button.getText());
  }

  @Test(expected = WidgetNotFoundException.class)
  public void testAccountsArea_zeroAccounts() {
    setUpLoginService();

    AccountsPanel panel = new AccountsPanel(null, loginService);
    Control control = panel.createDialogArea(shell);

    new SWTBot(control).labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "email");
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testAccountsArea_oneAccount() {
    setUpLoginService(Arrays.asList(account1));

    AccountsPanel panel = new AccountsPanel(null, loginService);
    Control control = panel.createDialogArea(shell);

    SWTBot bot = new SWTBot(control);
    SWTBotLabel email = bot.labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "email", 0);
    SWTBotLabel name = bot.labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "accountName", 0);
    assertEquals("alice@example.com", email.getText());
    assertEquals("Alice", name.getText());

    bot.labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "email", 1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testAccountsArea_accountWithNullName() {
    setUpLoginService(Arrays.asList(account2));

    AccountsPanel panel = new AccountsPanel(null, loginService);
    Control control = panel.createDialogArea(shell);

    SWTBot bot = new SWTBot(control);
    SWTBotLabel email = bot.labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "email", 0);
    SWTBotLabel name = bot.labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "accountName", 0);
    assertEquals("bob@example.com", email.getText());
    assertTrue(name.getText().isEmpty());

    bot.labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "email", 1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testAccountsArea_threeAccounts() {
    setUpLoginService(Arrays.asList(account1, account2, account3));

    AccountsPanel panel = new AccountsPanel(null, loginService);
    Control control = panel.createDialogArea(shell);

    SWTBot bot = new SWTBot(control);
    List<String> emails = Arrays.asList(
        bot.labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "email", 0).getText(),
        bot.labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "email", 1).getText(),
        bot.labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "email", 2).getText());
    List<String> names = Arrays.asList(
        bot.labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "accountName", 0).getText(),
        bot.labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "accountName", 1).getText(),
        bot.labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "accountName", 2).getText());

    assertTrue(emails.contains("alice@example.com"));
    assertTrue(emails.contains("bob@example.com"));
    assertTrue(emails.contains("charlie@example.com"));
    assertTrue(names.contains("Alice"));
    assertTrue(names.contains(""));
    assertTrue(names.contains("Charlie"));

    bot.labelWithId(AccountsPanel.CSS_CLASS_NAME_KEY, "email", 3);
  }

  private void setUpLoginService(List<Account> accounts) {
    when(loginService.hasAccounts()).thenReturn(!accounts.isEmpty());
    when(loginService.getAccounts()).thenReturn(new HashSet<>(accounts));
  }

  private void setUpLoginService() {
    setUpLoginService(new ArrayList<Account>());  // Simulate no signed-in account.
  }
}
