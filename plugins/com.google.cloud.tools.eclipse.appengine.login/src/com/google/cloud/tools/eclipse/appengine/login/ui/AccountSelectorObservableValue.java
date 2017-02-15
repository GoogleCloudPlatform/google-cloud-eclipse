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

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/**
 * An object that represents and binds to the email value of an {@link AccountSelector}, whose
 * changes (i.e., account selection changes in the combo box) can be tracked by value change
 * listeners.
 */
public class AccountSelectorObservableValue extends AbstractObservableValue {

  private String oldValue;
  private AccountSelector accountSelector;

  public AccountSelectorObservableValue(final AccountSelector accountSelector) {
    super(DisplayRealm.getRealm(accountSelector.getDisplay()));
    this.accountSelector = accountSelector;

    accountSelector.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        String newValue = accountSelector.getSelectedEmail();
        fireValueChange(Diffs.createValueDiff(oldValue, newValue));
        oldValue = newValue;
      }
    });
  }

  @Override
  public Object getValueType() {
    return String.class;
  }

  @Override
  protected Object doGetValue() {
    return accountSelector.getSelectedEmail();
  }

  @Override
  protected void doSetValue(final Object value) {
    accountSelector.selectAccount((String) value);
  }
}
