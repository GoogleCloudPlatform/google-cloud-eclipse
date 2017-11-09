/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.login;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class PackageImportTest {
  
  // This test exercises the local Jetty server used to receive the authentication code and
  // ensures, that required packages (thus classes) are imported in MANIFEST.MF
  @Test
  public void testRequiredPackagesImported() throws IOException {
    LocalServerReceiver codeReceiver = new LocalServerReceiver();
    String redirectUri = codeReceiver.getRedirectUri();
    URLConnection connection = new URL(redirectUri).openConnection();
    connection.setDoOutput(true);
    try (OutputStream outputStream = connection.getOutputStream();
        InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
      outputStream.write("hello".getBytes(StandardCharsets.UTF_8));
      StringBuilder response = new StringBuilder();
      char[] buffer = new char[1024];
      while (reader.read(buffer) != -1) {
        response.append(buffer);
      }
      assertThat(response.toString(), containsString("OAuth 2.0 Authentication Token Received"));
    }
  }
}
