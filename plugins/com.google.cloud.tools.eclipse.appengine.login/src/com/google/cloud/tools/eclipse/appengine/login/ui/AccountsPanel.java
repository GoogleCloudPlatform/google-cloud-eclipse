/*******************************************************************************
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
 *******************************************************************************/

package com.google.cloud.tools.eclipse.appengine.login.ui;

import com.google.cloud.tools.eclipse.appengine.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.appengine.login.Messages;
import com.google.cloud.tools.ide.login.Account;
import com.google.common.annotations.VisibleForTesting;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.List;

/**
 * A panel listing all currently logged-in accounts. The panel allows adding new accounts and
 * logging out all accounts.
 */
public class AccountsPanel extends PopupDialog {

  private IGoogleLoginService loginService;

  @VisibleForTesting Button logOutButton;
  @VisibleForTesting List<Label> accountLabels = new ArrayList<>();

  public AccountsPanel(Shell parent, IGoogleLoginService loginService) {
    super(parent, SWT.MODELESS,
        true /* takeFocusOnOpen */,
        false /* persistSize */,
        false /* persistLocation */,
        false /* showDialogMenu */,
        false /* showPersistActions */,
        null /* no title area */, null /* no info text area */);
    this.loginService = loginService;
  }

  @Override
  protected Color getBackground() {
    return getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
  }

  @Override
  protected Color getForeground() {
    return getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);
    GridLayoutFactory.swtDefaults().generateLayout(container);

    createAccountsPane(container);
    createButtons(container);
    return container;
  }

  @VisibleForTesting
  void createAccountsPane(Composite container) {
    for (Account account : loginService.getAccounts()) {
      Label label = new Label(container, SWT.NONE);
      label.setText(account.getEmail());
      accountLabels.add(label);
    }
  }

  private void createButtons(Composite container) {
    Composite buttonArea = new Composite(container, SWT.NONE);
    GridLayoutFactory.fillDefaults().numColumns(2).applyTo(buttonArea);

    Button addAccountButton = new Button(buttonArea, SWT.FLAT);
    addAccountButton.setText(Messages.BUTTON_ACCOUNTS_PANEL_ADD_ACCOUNT);
    addAccountButton.addSelectionListener(new LogInOnClick());
    GridDataFactory.defaultsFor(addAccountButton).applyTo(addAccountButton);

    if (loginService.hasAccounts()) {
      logOutButton = new Button(buttonArea, SWT.FLAT);
      logOutButton.setText(Messages.BUTTON_ACCOUNTS_PANEL_LOGOUT);
      logOutButton.addSelectionListener(new LogOutOnClick());
      GridDataFactory.defaultsFor(logOutButton).applyTo(logOutButton);
    }
  }

  private class LogInOnClick extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent event) {
      close();
      loginService.logIn(null /* no custom dialog title */);
    }
  };

  private class LogOutOnClick extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent event) {
      String[] dialogButtonLabels = {"Sign Out", "Don't Sign Out"};
      MessageDialog logoutDialog = new MessageDialog(
          getShell(),
          Messages.LOGOUT_CONFIRM_DIALOG_TITLE,
          null /* dialogTitleImage */,
          Messages.LOGOUT_CONFIRM_DIALOG_MESSAGE,
          MessageDialog.QUESTION,
          dialogButtonLabels, 0);
      boolean shouldLogout = logoutDialog.open() == 0;
      if (shouldLogout) {
        close();
        loginService.logOutAll();
      }
    }
  };
}
