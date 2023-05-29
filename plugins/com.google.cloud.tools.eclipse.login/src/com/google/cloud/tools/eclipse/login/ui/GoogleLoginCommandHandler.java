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

import com.google.cloud.tools.eclipse.googleapis.internal.GoogleApiFactory;
import com.google.cloud.tools.eclipse.login.Messages;
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
    new AccountsPanel(HandlerUtil.getActiveShell(event)).open();
    return null;
  }
  
  @Override
  public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
    element.setText(GoogleApiFactory.INSTANCE.getCredential().isPresent() ? Messages.getString("LOGIN_MENU_LOGGED_IN")
        : Messages.getString("LOGIN_MENU_LOGGED_OUT"));
  }

  
}
