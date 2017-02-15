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
