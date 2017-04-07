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

package com.google.cloud.tools.eclipse.login;

import static org.junit.Assert.assertEquals;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.gson.Gson;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CredentialHelperTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testToJsonFile() throws IOException {
    Credential credential = createCredential("fake_access_token", "fake_refresh_token");
    Path jsonFile = tempFolder.getRoot().toPath().resolve("credential-for-gcloud.json");
    CredentialHelper.toJsonFile(credential, jsonFile);

    try (InputStream in = new FileInputStream(jsonFile.toFile());
        Reader reader = new InputStreamReader(in)) {
      CredentialType credentialType = new Gson().fromJson(reader, CredentialType.class);
      assertEquals(Constants.getOAuthClientId(), credentialType.client_id);
      assertEquals(Constants.getOAuthClientSecret(), credentialType.client_secret);
      assertEquals("fake_refresh_token", credentialType.refresh_token);
      assertEquals("authorized_user", credentialType.type);
    }
  }

  private class CredentialType {
    private String client_id;
    private String client_secret;
    private String refresh_token;
    private String type;
  };

  private Credential createCredential(String accessToken, String refreshToken) {
    GoogleCredential credential = new GoogleCredential.Builder()
        .setTransport(new NetHttpTransport())
        .setJsonFactory(new JacksonFactory())
        .setClientSecrets(Constants.getOAuthClientId(), Constants.getOAuthClientSecret())
        .build();
    credential.setAccessToken(accessToken);
    credential.setRefreshToken(refreshToken);
    return credential;
  }
}
