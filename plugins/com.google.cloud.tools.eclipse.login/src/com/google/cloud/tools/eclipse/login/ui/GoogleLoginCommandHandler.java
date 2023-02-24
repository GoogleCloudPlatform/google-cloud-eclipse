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

import com.google.cloud.tools.eclipse.googleapis.IGoogleApiFactory;
import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.login.IGoogleLoginService;
import com.google.cloud.tools.eclipse.login.Messages;
import com.google.cloud.tools.eclipse.ui.util.ServiceUtils;
import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

public class GoogleLoginCommandHandler extends AbstractHandler implements IElementUpdater {

  static final IGoogleApiFactory apiFactory = new GoogleApiFactory();
  static final long LOGGED_IN_CHECK_DELAY = 2000l; // update logged in every 2 seconds
  static boolean isLoggedIn;
  Timer loggedInCheckTask = new Timer();
  
  public GoogleLoginCommandHandler() {
    loggedInCheckTask.schedule(new TimerTask() {

      @Override
      public void run() {
        checkIsLoggedIn();
      }
      
    }, 0l /*delay*/, LOGGED_IN_CHECK_DELAY);
  }
  
  @Override
  public boolean isEnabled() {
    return isLoggedIn;
  }
  
  private void checkIsLoggedIn() {
    boolean loggedIn;
    try {
      loggedIn = apiFactory.getAccount() != null;
    } catch (IOException ex) {
      loggedIn = false;
    }
    isLoggedIn = loggedIn;
    setEnabled(loggedIn);
  }
  
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IGoogleLoginService loginService = ServiceUtils.getService(event, IGoogleLoginService.class);

    if (isEnabled()) {
      new AccountsPanel(HandlerUtil.getActiveShell(event), loginService).open();
    }

    return null;
  }
  
  @Override
  public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
    IGoogleLoginService loginService =
        element.getServiceLocator().getService(IGoogleLoginService.class);
    
    element.setText(isEnabled() ? Messages.getString("LOGIN_MENU_LOGGED_IN")
        : Messages.getString("LOGIN_MENU_LOGGED_OUT"));
  }

  
}
