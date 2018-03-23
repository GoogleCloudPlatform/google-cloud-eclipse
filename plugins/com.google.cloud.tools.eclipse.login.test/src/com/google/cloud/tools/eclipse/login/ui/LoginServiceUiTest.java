/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.login.ui;

import static org.junit.Assert.assertEquals;

import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.Test;

public class LoginServiceUiTest {

  @Test
  public void testLoginSuccessPage() throws IOException {
    LocalServerReceiver server = LoginServiceUi.createLocalServerReceiver();
    try {
      String localServerUrl = server.getRedirectUri();
      String landingPageUrl = simulateLogin(localServerUrl + "?code=success-simulated");
      assertEquals("https://cloud.google.com/eclipse/auth_success", landingPageUrl);
    } finally {
      server.stop();
    }
  }

  @Test
  public void testLoginFailurePage() throws IOException {
    LocalServerReceiver server = LoginServiceUi.createLocalServerReceiver();
    try {
      String localServerUrl = server.getRedirectUri();
      String landingPageUrl = simulateLogin(localServerUrl + "?error=simulated");
      assertEquals("https://cloud.google.com/eclipse/auth_failure", landingPageUrl);
    } finally {
      server.stop();
    }
  }

  private String simulateLogin(String urlString) throws IOException {
    URL url = new URL(urlString);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    try {
      int code = connection.getResponseCode();
      assertEquals(302, code);  // common code for URL redirection
      return connection.getHeaderField("Location");
    } finally {
      connection.disconnect();
    }
  }
}
