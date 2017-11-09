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

import com.google.api.client.auth.oauth2.Credential;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to work with {@link Credential} objects.
 */
public class CredentialHelper {

  private static final String CLIENT_ID_LABEL = "client_id";
  private static final String CLIENT_SECRET_LABEL = "client_secret";
  private static final String REFRESH_TOKEN_LABEL = "refresh_token";
  private static final String GCLOUD_USER_TYPE_LABEL = "type";
  private static final String GCLOUD_USER_TYPE = "authorized_user";

  /**
   * Writes a {@link Credential} object to a file in the JSON format expected by gcloud's
   * {@code credential-file-override} feature.
   */
  public static void toJsonFile(Credential credential, Path toFile) throws IOException {
    Map<String, String> credentialMap = new HashMap<>();
    credentialMap.put(CLIENT_ID_LABEL, Constants.getOAuthClientId());
    credentialMap.put(CLIENT_SECRET_LABEL, Constants.getOAuthClientSecret());
    credentialMap.put(REFRESH_TOKEN_LABEL, credential.getRefreshToken());
    credentialMap.put(GCLOUD_USER_TYPE_LABEL, GCLOUD_USER_TYPE);

    String json = new Gson().toJson(credentialMap);
    Files.write(toFile, json.getBytes(StandardCharsets.UTF_8));
  }
}
