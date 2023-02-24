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

import com.google.cloud.tools.eclipse.googleapis.Account;
import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A panel listing all currently logged-in accounts. The panel allows adding new accounts and
 * logging out all accounts.
 */
public class AccountsPanel extends PopupDialog {

  private static final Logger logger = Logger.getLogger(AccountsPanel.class.getName());

  private final IGoogleLoginService loginService;
  private final LabelImageLoader imageLoader;
  private final IGoogleApiFactory apiFactory = new GoogleApiFactory();

  public AccountsPanel(Shell parent, IGoogleLoginService loginService) {
    this(parent, loginService, new LabelImageLoader());
  }

  @VisibleForTesting
  AccountsPanel(Shell parent, IGoogleLoginService loginService, LabelImageLoader imageLoader) {
    super(parent, SWT.MODELESS,
        true /* takeFocusOnOpen */,
        false /* persistSize */,
        false /* persistLocation */,
        false /* showDialogMenu */,
        false /* showPersistActions */,
        null /* no title area */, null /* no info text area */);
    this.loginService = loginService;
    this.imageLoader = imageLoader;
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
    return container;
  }

  @VisibleForTesting
  void createAccountsPane(Composite accountArea) {
    List<Account> accounts;
    try {
      accounts = Collections.singletonList(apiFactory.getAccount());
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Could not obtain ADC account", ex);
      accounts = Collections.emptyList();
    }
    for (Account account : accounts) {
      Composite accountRow = new Composite(accountArea, SWT.NONE);
      Label avatar = new Label(accountRow, SWT.NONE);
      Composite secondColumn = new Composite(accountRow, SWT.NONE);
      Label name = new Label(secondColumn, SWT.LEAD);
      Label email = new Label(secondColumn, SWT.LEAD);
      Label separator = new Label(accountArea, SWT.HORIZONTAL | SWT.SEPARATOR);
      separator.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

      // <Avatar size> = 3 * <email label height>
      Point emailSize = email.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      int avatarSize = emailSize.y * 3;

      GridDataFactory.swtDefaults().hint(avatarSize, avatarSize).applyTo(avatar);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(accountRow);
      GridLayoutFactory.fillDefaults().generateLayout(secondColumn);

      if (account.getName().isPresent()) {
        name.setText(account.getName().get());
      }
      email.setText(account.getEmail());  // email is never null.

      if (account.getAvatarUrl().isPresent()) {
        try {
          String url = resizedImageUrl(account.getAvatarUrl().get(), avatarSize);
          imageLoader.loadImage(url, avatar);
        } catch (MalformedURLException ex) {
          logger.log(Level.WARNING, "malformed avatar image URL", ex);
        }
      }
    }
  }

  /**
   * Apply a resize directive to an Google avatar URL, replacing any previous resize directives.
   *
   * @param avatarUrl the URL
   * @param avatarSize the desired image size
   * @return an amended URL
   */
  @VisibleForTesting
  static String resizedImageUrl(String avatarUrl, int avatarSize) {
    int index = avatarUrl.lastIndexOf('=');
    if (index > 0) {
      avatarUrl = avatarUrl.substring(0, index);
    }
    return avatarUrl + "=s" + avatarSize;
  }

  private class LogInOnClick extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent event) {
      close();
      loginService.logIn();
    }
  }

  private class LogOutOnClick extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent event) {
      close();
      loginService.logOutAll();
    }
  }
}
