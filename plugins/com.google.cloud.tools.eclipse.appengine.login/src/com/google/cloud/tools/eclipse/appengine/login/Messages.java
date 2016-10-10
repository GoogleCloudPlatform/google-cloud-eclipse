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

package com.google.cloud.tools.eclipse.appengine.login;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.google.cloud.tools.eclipse.appengine.login.messages"; //$NON-NLS-1$
  public static String BUTTON_ACCOUNTS_PANEL_ADD_ACCOUNT;
  public static String BUTTON_ACCOUNTS_PANEL_LOGOUT;
  public static String LOGIN_ERROR_CANNOT_OPEN_BROWSER;
  public static String LOGIN_ERROR_DIALOG_MESSAGE;
  public static String LOGIN_ERROR_DIALOG_TITLE;
  public static String LOGIN_ERROR_LOCAL_SERVER_RUN;
  public static String LOGIN_MENU_LOGGED_IN;
  public static String LOGIN_MENU_LOGGED_OUT;
  public static String LOGIN_PROGRESS_DIALOG_MESSAGE;
  public static String LOGIN_PROGRESS_DIALOG_TITLE;
  public static String LOGOUT_CONFIRM_DIALOG_MESSAGE;
  public static String LOGOUT_CONFIRM_DIALOG_TITLE;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
