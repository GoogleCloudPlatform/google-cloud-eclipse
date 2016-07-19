package com.google.cloud.tools.eclipse.appengine.login;

import org.eclipse.core.runtime.Platform;

/**
 * Placeholder constants initialized at compile-time.
 */
public class Constants {

  private static final String OAUTH_CLIENT_ID = "@oauth.client.id@";
  private static final String OAUTH_CLIENT_SECRET = "@oauth.client.secret@";

  public static String getOAuthClientId() {
    if (Platform.inDevelopmentMode()) {
      return System.getProperty("oauth.client.id", "(unset:oauth.client.id)");
    } else {
      return OAUTH_CLIENT_ID;
    }
  }

  public static String getOAuthClientSecret() {
    if (Platform.inDevelopmentMode()) {
      return System.getProperty("oauth.client.secret", "(unset:oauth.client.secret)");
    } else {
      return OAUTH_CLIENT_SECRET;
    }
  }
}
