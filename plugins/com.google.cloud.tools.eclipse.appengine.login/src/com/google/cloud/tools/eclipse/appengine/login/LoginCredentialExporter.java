package com.google.cloud.tools.eclipse.appengine.login;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.IPath;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;

/**
 * Provides helper method to save an OAuth {@link Credential} object
 *
 * @see CredentialHelper
 */
public class LoginCredentialExporter {

  private static final String CREDENTIAL_FILENAME = "gcloud-credentials.json";
  
  private CredentialHelper credentialHelper;
  
  public LoginCredentialExporter() {
    this(new CredentialHelper());
  }
  
  @VisibleForTesting
  LoginCredentialExporter(CredentialHelper credentialHelper) {
    this.credentialHelper = credentialHelper;
  }

  /**
   * Saves the OAuth credentials to a JSON file under <code>workDirectory</code>
   *
   * @param workDirectory the destination for the JSON file containing the received OAuth credential
   * @param credential to be saved in JSON format
   * @return the path of the saved credential file
   * @throws IOException if the JSON file with the credential cannot be created
   */
  public Path saveCredential(IPath workDirectory, Credential credential) throws IOException {
    String jsonCredential = credentialHelper.toJson(credential);
    return Files.write(workDirectory.append(CREDENTIAL_FILENAME).toFile().toPath(),
                       jsonCredential.getBytes(Charsets.UTF_8));
  }
}
