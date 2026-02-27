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
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.login.Messages;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener;
import com.google.cloud.tools.eclipse.ui.util.event.OpenUriSelectionListener.ErrorDialogErrorHandler;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

public class AccountSelector extends Composite {

  private ListenerList<Runnable> selectionListeners = new ListenerList<>();
  private Optional<Account> prevAccount = Optional.empty();
  private static Logger logger = Logger.getLogger(AccountSelector.class.getName());
  private Runnable onCredentialChange = this::forceAccountCheck;

  @VisibleForTesting Link accountEmail;

  public AccountSelector(Composite parent) {
    super(parent, SWT.NONE);

    Composite accountEmailComposite = new Composite(this, SWT.NONE);
    accountEmail = new Link(accountEmailComposite, SWT.WRAP);
    updateEmail();
    
    GridDataFactory.fillDefaults().grab(true, false).applyTo(this);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(accountEmailComposite);
    GridLayoutFactory.fillDefaults().generateLayout(accountEmailComposite);
    GridLayoutFactory.fillDefaults().generateLayout(this);
    GoogleApiFactory.INSTANCE.addCredentialChangeListener(onCredentialChange);
    addDisposeListener(new DisposeListener() {
      
      @Override
      public void widgetDisposed(DisposeEvent e) {
        GoogleApiFactory.INSTANCE.removeCredentialChangeListener(onCredentialChange);
      }
    });
    
  }
  
  private void updateEmail() {
    if (GoogleApiFactory.INSTANCE.getCredential().isPresent()) {
      accountEmail.setText(getSelectedEmail());
    } else {
      accountEmail.setText(Messages.getString("NO_ADC_DETECTED_MESSAGE") + ". " + Messages.getString("NO_ADC_DETECTED_LINK"));
      accountEmail.addSelectionListener(new OpenUriSelectionListener(
           () -> Collections.emptyMap() ,
           new ErrorDialogErrorHandler(getShell())));
    }
  }
  
  /**
   * @return true if this selector lists an account with {@code email}. For convenience of
   *     callers, {@code email} may be {@code null} or empty, which always returns false
   */
  public boolean isEmailAvailable(String email) {
    return !Strings.isNullOrEmpty(email) && getSelectedEmail() == email;
  }

  /**
   * Returns a {@link Credential} object associated with the account, if selected. Otherwise,
   * {@code null}.
   *
   * Note that, if an account is selected, the returned {@link Credential} cannot be {@code null}.
   * (By its contract, {@link Account} never carries a {@code null} {@link Credential}.)
   */
  public Credential getSelectedCredential() {
    return getSelectedAccount().map(Account::getOAuth2Credential).orElse(null);
  }

  /**
   * Returns the currently selected email, or empty string if none; never {@code null}.
   */
  public String getSelectedEmail() {
    Optional<Account> account = getSelectedAccount();
    return account.isPresent() ? account.get().getEmail() : "";
  }
  
  private Optional<Account> getSelectedAccount() {
    Optional<Account> account = GoogleApiFactory.INSTANCE.getAccount();
    if (!account.equals(prevAccount)) {
      prevAccount = account;
      updateEmail();
      fireSelectionListeners();
    }
    return account;
  }

  public boolean isSignedIn() {
    return getSelectedAccount().isPresent();
  }

  public void addSelectionListener(Runnable listener) {
    selectionListeners.add(listener);
  }

  public void removeSelectionListener(Runnable listener) {
    selectionListeners.remove(listener);
  }

  /**
   * used to trigger selection listeners in tests
   */
  public boolean forceAccountCheck() {
    Optional<Account> prev = prevAccount;
    getSelectedAccount();
    if (!prev.equals(prevAccount)) {
      logger.log(Level.FINE, "forceAccountCheck() detected an account change");
      return true;
    }
    return false;
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

  /**
   * @return 1 if logged in, 0 if not
   */
  public int getAccountCount() {
    return isSignedIn() ? 1 : 0;
  }
  
  private void fireSelectionListeners() {
     for (Object o : selectionListeners.getListeners()) {
       ((Runnable) o).run();
    }
  }
}
