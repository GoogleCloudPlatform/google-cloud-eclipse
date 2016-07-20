package com.google.cloud.tools.eclipse.appengine.login;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.google.cloud.tools.eclipse.appengine.login.messages"; //$NON-NLS-1$
  public static String ENTER_VERIFICATION_CODE_DIALOG_MESSAGE;
  public static String ENTER_VERIFICATION_CODE_DIALOG_TITLE;
  public static String ERROR_BROWSER_LAUNCHING;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
