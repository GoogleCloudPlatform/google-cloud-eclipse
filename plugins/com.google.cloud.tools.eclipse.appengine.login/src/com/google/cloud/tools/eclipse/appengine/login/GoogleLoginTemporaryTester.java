/*******************************************************************************
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.login;

import com.google.api.client.auth.oauth2.Credential;
import com.google.gson.Gson;

import org.eclipse.core.commands.ExecutionException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

// FIXME This class is for manual integration login test. Remove it in the final product.
public class GoogleLoginTemporaryTester {

  public boolean testLogin() throws ExecutionException {
    try {
      File credentialFile = getCredentialFile();
      return credentialFile != null && testCredentialWithGcloud(credentialFile);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return false;
    }
  }

  private static final String CLIENT_ID_LABEL = "client_id";
  private static final String CLIENT_SECRET_LABEL = "client_secret";
  private static final String REFRESH_TOKEN_LABEL = "refresh_token";
  private static final String GCLOUD_USER_TYPE_LABEL = "type";
  private static final String GCLOUD_USER_TYPE = "authorized_user";

  private File getCredentialFile() throws IOException {
    Credential credential = new GoogleLoginService().getActiveCredential();
    if (credential == null) {
      return null;
    }

    Map<String, String> credentialMap = new HashMap<>();
    credentialMap.put(CLIENT_ID_LABEL, Constants.getOAuthClientId());
    credentialMap.put(CLIENT_SECRET_LABEL, Constants.getOAuthClientSecret());
    credentialMap.put(REFRESH_TOKEN_LABEL, credential.getRefreshToken());
    credentialMap.put(GCLOUD_USER_TYPE_LABEL, GCLOUD_USER_TYPE);

    String jsonCredential = new Gson().toJson(credentialMap);

    File credentialFile = File.createTempFile("tmp_eclipse_login_test_cred", ".json");
    credentialFile.deleteOnExit();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(credentialFile))) {
      writer.write(jsonCredential);
    }
    return credentialFile;
  }

  private boolean testCredentialWithGcloud(File credentialFile) throws IOException {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(
          "gcloud", "projects", "list", "--credential-file-override=" + credentialFile.toString());

      Process process = processBuilder.start();
      process.waitFor();

      try (
        BufferedReader outReader =
            new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errReader =
            new BufferedReader(new InputStreamReader(process.getErrorStream()))
      ) {
        while (outReader.ready() || errReader.ready()) {
          if (outReader.ready()) {
            System.out.println("[stdout] " + outReader.readLine());
          }
          if (errReader.ready()) {
            System.out.println("[stderr] " + errReader.readLine());
          }
        }
      }
      return process.exitValue() == 0;

    } catch (InterruptedException ie) {
      ie.printStackTrace();
      return false;
    }
  }
}
