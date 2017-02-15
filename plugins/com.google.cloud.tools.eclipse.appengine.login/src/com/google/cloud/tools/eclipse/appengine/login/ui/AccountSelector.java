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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.Strings;
import com.google.cloud.tools.eclipse.appengine.login.IGoogleLoginService;
import com.google.cloud.tools.ide.login.Account;
import com.google.common.annotations.VisibleForTesting;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

public class AccountSelector extends Composite {

  private IGoogleLoginService loginService;
  private String loginMessage;
  private Credential selectedCredential;

  @VisibleForTesting Combo combo;
  @VisibleForTesting LogInOnSelect logInOnSelect = new LogInOnSelect();

  public AccountSelector(Composite parent, IGoogleLoginService loginService,
                         String loginMessage) {
    super(parent, SWT.NONE);
    this.loginService = loginService;
    this.loginMessage = loginMessage;
    GridLayoutFactory.fillDefaults().generateLayout(this);

    combo = new Combo(this, SWT.READ_ONLY);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(combo);

    for (Account account : loginService.getAccounts()) {
      combo.add(account.getEmail());
      combo.setData(account.getEmail(), account.getOAuth2Credential());
    }
    combo.add(loginMessage);
    combo.addSelectionListener(logInOnSelect);
  }

  /**
   * @return true if this selector lists an account with {@code email}. For convenience of
   *     callers, {@code email} may be {@code null} or empty, which always returns false
   */
  public boolean isEmailAvailable(String email) {
    return !Strings.isNullOrEmpty(email) && combo.indexOf(email) != -1;
  }

  /**
   * Returns a {@link Credential} object associated with the account, if selected. Otherwise,
   * {@code null}.
   *
   * Note that, if an account is selected, the returned {@link Credential} cannot be {@code null}.
   * (By its contract, {@link Account} never carries a {@code null} {@link Credential}.)
   */
  public Credential getSelectedCredential() {
    return selectedCredential;
  }

  public String getSelectedEmail() {
    return combo.getText();
  }

  /**
   * Selects an account corresponding to the given {@code email} and returns its index of the
   * combo item. If there is no account corresponding to the {@code email}, returns -1 while
   * retaining current selection (if any).
   *
   * @param email email address to use to select an account. If {@code null} or the empty string,
   *     or if there is no matching account, this method does nothing
   * @return index of the newly selected combo item; -1 if {@code email} is {@code null} or
   *     the empty string, or if there is no matching account
   */
  public int selectAccount(String email) {
    int index = Strings.isNullOrEmpty(email) ? -1 : combo.indexOf(email);
    if (index != -1) {
      combo.select(index);
      selectedCredential = (Credential) combo.getData(email);
    }
    return index;
  }

  @VisibleForTesting
  class LogInOnSelect extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent event) {
      if (combo.getText().equals(loginMessage)) {
        Account account = loginService.logIn(null /* no custom dialog message */);
        if (account != null) {
          addAndSelectAccount(account);
        } else {
          combo.deselectAll();
        }
      }

      selectedCredential = (Credential) combo.getData(getSelectedEmail());
    }

    private void addAndSelectAccount(Account account) {
      // If the combo already has the email, just select it.
      if (selectAccount(account.getEmail()) != -1) {
        return;
      }
      combo.add(account.getEmail(), 0 /* place at top */);
      combo.setData(account.getEmail(), account.getOAuth2Credential());
      combo.select(0);
    }
  }

  public void addSelectionListener(SelectionListener listener) {
    combo.addSelectionListener(listener);
  }

  public void removeSelectionListener(SelectionListener listener) {
    combo.removeSelectionListener(listener);
  }
}
