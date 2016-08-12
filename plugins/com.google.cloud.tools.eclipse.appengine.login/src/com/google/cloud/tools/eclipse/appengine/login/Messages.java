package com.google.cloud.tools.eclipse.appengine.login;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.google.cloud.tools.eclipse.appengine.login.messages"; //$NON-NLS-1$
  public static String LOGIN_ERROR_CANNOT_OPEN_BROWSER;
  public static String LOGIN_ERROR_DIALOG_MESSAGE;
  public static String LOGIN_ERROR_DIALOG_TITLE;
  public static String LOGIN_ERROR_IN_PROGRESS;
  public static String LOGIN_ERROR_LOCAL_SERVER_RUN;
  public static String LOGIN_MENU_LOGGED_IN;
  public static String LOGIN_MENU_LOGGED_OUT;
  public static String LOGIN_PROGRESS_DIALOG_MESSAGE;
  public static String LOGIN_PROGRESS_DIALOG_TITLE;
  public static String LOGIN_TOOLTIP_LOGGED_IN;
  public static String LOGIN_TOOLTIP_LOGGED_OUT;
  public static String LOGOUT_CONFIRM_DIALOG_MESSAGE;
  public static String LOGOUT_CONFIRM_DIALOG_TITILE;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
