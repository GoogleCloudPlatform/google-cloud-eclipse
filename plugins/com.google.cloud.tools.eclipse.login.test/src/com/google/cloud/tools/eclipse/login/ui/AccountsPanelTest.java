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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import com.google.cloud.tools.eclipse.test.util.TestAccountProvider;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider.State;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AccountsPanelTest {

  @Rule public ShellTestResource shellTestResource = new ShellTestResource();
  private Shell shell;
  
  @Mock private LabelImageLoader imageLoader;

  @Before
  public void setUp() {
    TestAccountProvider.setAsDefaultProvider(State.LOGGED_IN);
    shell = shellTestResource.getShell();
  }

  @Test
  public void testLogOutButton_notLoggedIn() {

    TestAccountProvider.setProviderState(State.NOT_LOGGED_IN);
    
    AccountsPanel panel = new AccountsPanel(null, imageLoader);
    Control control = panel.createDialogArea(shell);

    List<String> buttonTexts = collectButtonTexts((Composite) control);
    assertEquals(0, buttonTexts.size());
  }

  @Test
  public void testLogOutButton_loggedIn() {

    AccountsPanel panel = new AccountsPanel(null, imageLoader);
    Control control = panel.createDialogArea(shell);

    List<String> buttonTexts = collectButtonTexts((Composite) control);
    assertEquals(0, buttonTexts.size());
  }

  @Test
  public void testAccountsArea_zeroAccounts() {

    TestAccountProvider.setProviderState(State.NOT_LOGGED_IN);
    
    AccountsPanel panel = new AccountsPanel(null, imageLoader);
    Control control = panel.createDialogArea(shell);

    NamesEmails namesEmails = collectNamesEmails(control);
    assertTrue(namesEmails.emails.isEmpty());
  }


  @Test
  public void testAccountsArea_accountWithNullName() {

    AccountsPanel panel = new AccountsPanel(null, imageLoader);
    Control control = panel.createDialogArea(shell);

    NamesEmails namesEmails = collectNamesEmails(control);
    assertEquals(1, namesEmails.emails.size());
    assertEquals(TestAccountProvider.EMAIL_ACCOUNT_1, namesEmails.emails.get(0));
    assertEquals(TestAccountProvider.NAME_ACCOUNT_1, namesEmails.names.get(0));
  }

  @Test
  public void testAccountsArea_avatarImageUrl() throws MalformedURLException {

    AccountsPanel panel = new AccountsPanel(null, imageLoader);
    panel.createDialogArea(shell);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(imageLoader).loadImage(captor.capture(), any());
    assertEquals(1, captor.getAllValues().size());

    Pattern urlPattern = Pattern.compile("^https://avatar.url/account1=s([0-9]+)$");
    Matcher matcher = urlPattern.matcher(captor.getValue());
    assertTrue(matcher.find());
    assertThat(Integer.valueOf(matcher.group(1)), Matchers.greaterThan(0));
  }

  @Test
  public void testAccountsArea_threeAccounts() {

    TestAccountProvider.setProviderState(State.LOGGED_IN_SECOND_ACCOUNT);
    
    AccountsPanel panel = new AccountsPanel(null, imageLoader);
    Control control = panel.createDialogArea(shell);

    NamesEmails namesEmails = collectNamesEmails(control);
    assertEquals(1, namesEmails.emails.size());
    assertTrue(namesEmails.emails.contains(TestAccountProvider.EMAIL_ACCOUNT_2));
    assertTrue(namesEmails.names.contains(TestAccountProvider.NAME_ACCOUNT_2));
  }

  @Test
  public void testResizedImageUrl() {
    assertEquals("https://lh3/xxxx=s48", AccountsPanel.resizedImageUrl("https://lh3/xxxx", 48));
    assertEquals(
        "https://lh3/xxxx=s48", AccountsPanel.resizedImageUrl("https://lh3/xxxx=s96-c", 48));
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
    if (!TestAccountProvider.INSTANCE.getCredential().isPresent()) {
      return namesEmails;
    }
    Control[] controls = ((Composite) dialogArea).getChildren();
    for (int i = 0; i + 2 <= controls.length; i += 2) {
      Composite accountRow = (Composite) controls[i];
      assertTrue(accountRow.getChildren().length >= 2);
      Composite secondColumn = (Composite) accountRow.getChildren()[1];
      Control[] labels = secondColumn.getChildren();
      assertTrue(labels.length >= 2);
      namesEmails.names.add(((Label) labels[0]).getText());
      namesEmails.emails.add(((Label) labels[1]).getText());

      assertEquals(SWT.SEPARATOR, ((Label) controls[i+1]).getStyle() & SWT.SEPARATOR);
    }
    return namesEmails;
  }
}
