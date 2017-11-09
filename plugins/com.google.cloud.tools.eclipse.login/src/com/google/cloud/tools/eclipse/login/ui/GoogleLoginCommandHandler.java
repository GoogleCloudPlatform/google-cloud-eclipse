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

import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.login.Messages;
import com.google.cloud.tools.eclipse.ui.util.ServiceUtils;
import java.util.Map;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

public class GoogleLoginCommandHandler extends AbstractHandler implements IElementUpdater {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IGoogleLoginService loginService = ServiceUtils.getService(event, IGoogleLoginService.class);

    if (!loginService.hasAccounts()) {
      loginService.logIn(null /* no custom dialog message */);
    } else {
      new AccountsPanel(HandlerUtil.getActiveShell(event), loginService).open();
    }

    return null;
  }

  @Override
  public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
    IGoogleLoginService loginService =
        element.getServiceLocator().getService(IGoogleLoginService.class);
    boolean loggedIn = loginService.hasAccounts();

    element.setText(loggedIn ? Messages.getString("LOGIN_MENU_LOGGED_IN")
        : Messages.getString("LOGIN_MENU_LOGGED_OUT"));
  }
}
