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

import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.swt.widgets.Shell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

// FIXME This class is for manual integration login test. Remove it in the final product.
public class GoogleLoginTemporaryTester {

  public boolean testLogin(Shell shell) {
    try {
      File credentialFile = getCredentialFile(new SameShellProvider(shell));
      return credentialFile != null && testCredentialWithGcloud(credentialFile);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return false;
    }
  }

  private File getCredentialFile(IShellProvider shellProvider) throws IOException {
    Credential credential = new GoogleLoginService().getActiveCredential(shellProvider);
    if (credential == null) {
      return null;
    }

    File credentialFile = File.createTempFile("tmp_eclipse_login_test_cred", ".json");
    credentialFile.deleteOnExit();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(credentialFile))) {
      String jsonCredential = GoogleLoginService.getJsonCredential(credential);
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
