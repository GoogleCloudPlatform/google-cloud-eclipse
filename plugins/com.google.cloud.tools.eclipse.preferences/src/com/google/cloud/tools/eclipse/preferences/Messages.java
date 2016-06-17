package com.google.cloud.tools.eclipse.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.google.cloud.tools.eclipse.preferences.messages";
  public static String ANALYTICS_DISCLAIMER;
  public static String ANALYTICS_PREFERENCE_GROUP_TITLE;
  public static String ANALYTICS_OPT_IN_TEXT;
  public static String GOOGLE_PRIVACY_POLICY_URL;
  static {
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
