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

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.googleapis.Account;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.login.Messages;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.io.IOException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class AccountSelector extends Composite {

  private IGoogleApiFactory apiFactory;
  private Account selectedAccount;
  private ListenerList<Runnable> selectionListeners = new ListenerList<>();

  @VisibleForTesting Label accountEmail;

  public AccountSelector(Composite parent, IGoogleApiFactory apiFactory) {
    super(parent, SWT.NONE);
    this.apiFactory = apiFactory;

    accountEmail = new Label(this, SWT.READ_ONLY);
    
    if (apiFactory.isLoggedIn()) {
      accountEmail.setText(getSelectedAccountEmail());
    } else {
      accountEmail.setText(Messages.getString("ACCOUNT_SELECTOR_LOGGED_OUT"));
    }

    GridDataFactory.fillDefaults().grab(true, false).applyTo(accountEmail);
    GridLayoutFactory.fillDefaults().generateLayout(this);
  }

  String getSelectedAccountEmail() {
    try {
      return apiFactory.getAccount().getEmail();
    } catch (IOException ex) {
      return null;
    }
  }
  
  /**
   * @return true if this selector lists an account with {@code email}. For convenience of
   *     callers, {@code email} may be {@code null} or empty, which always returns false
   */
  public boolean isEmailAvailable(String email) {
    return !Strings.isNullOrEmpty(email) && accountEmail.getText() == email;
  }

  /**
   * Returns a {@link Credential} object associated with the account, if selected. Otherwise,
   * {@code null}.
   *
   * Note that, if an account is selected, the returned {@link Credential} cannot be {@code null}.
   * (By its contract, {@link Account} never carries a {@code null} {@link Credential}.)
   */
  public Credential getSelectedCredential() {
    return selectedAccount != null ? selectedAccount.getOAuth2Credential() : null;
  }

  /**
   * Returns the currently selected email, or empty string if none; never {@code null}.
   */
  public String getSelectedEmail() {
    return accountEmail.getText();
  }

  public boolean isSignedIn() {
    return apiFactory.isLoggedIn();
  }

  public void addSelectionListener(Runnable listener) {
    selectionListeners.add(listener);
  }

  public void removeSelectionListener(Runnable listener) {
    selectionListeners.remove(listener);
  }

  
  @Override
  public void setToolTipText(String string) {
    accountEmail.setToolTipText(string);
  }

  @Override
  public String getToolTipText() {
    return accountEmail.getToolTipText();
  }

  @Override
  public void setEnabled(boolean enabled) {
    accountEmail.setEnabled(enabled);
  }

  @Override
  public boolean getEnabled() {
    return accountEmail.getEnabled();
  }
}
