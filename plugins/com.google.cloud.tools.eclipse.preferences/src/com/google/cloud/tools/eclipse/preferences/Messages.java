package com.google.cloud.tools.eclipse.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.google.cloud.tools.eclipse.preferences.messages"; //$NON-NLS-1$
  public static String FIELD_EDITOR_ANALYTICS_DISCLAIMER;
  public static String FIELD_EDITOR_ANALYTICS_GROUP_TITLE;
  public static String FIELD_EDITOR_ANALYTICS_OPT_IN_TEXT;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
