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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.api.client.auth.oauth2.Credential;

// FIXME This class is for manual integration login test. Remove it in the final product.
public class GoogleLoginTemporaryTester {

  public void testLogin(Credential credential) throws IOException {
    File credentialFile = getCredentialFile(credential);
    testCredentialWithGcloud(credentialFile);
  }

  private File getCredentialFile(Credential credential) throws IOException {
    File credentialFile = File.createTempFile("tmp_eclipse_login_test_cred", ".json");
    credentialFile.deleteOnExit();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(credentialFile))) {
      String jsonCredential = new CredentialHelper().toJson(credential);
      writer.write(jsonCredential);
    }
    return credentialFile;
  }

  private void testCredentialWithGcloud(File credentialFile) throws IOException {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(
          "gcloud", "projects", "list", "--credential-file-override=" + credentialFile.toString());

      Process process = processBuilder.start();
      process.waitFor();

      String stdOut = new String();
      String stdErr = new String();
      try (
        BufferedReader outReader =
            new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errReader =
            new BufferedReader(new InputStreamReader(process.getErrorStream()))
      ) {
        while (outReader.ready() || errReader.ready()) {
          if (outReader.ready()) {
            stdOut += outReader.readLine();
          }
          if (errReader.ready()) {
            stdErr += errReader.readLine();
          }
        }
      }

      if (process.exitValue() != 0) {
        throw new IOException("non-zero exit code from gcloud:\n\n" + stdOut + "\n\n" + stdErr);
      }

    } catch (InterruptedException ie) {
      throw new IOException(ie);
    }
  }
}
