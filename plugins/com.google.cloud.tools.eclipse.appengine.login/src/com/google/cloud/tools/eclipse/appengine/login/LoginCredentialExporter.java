package com.google.cloud.tools.eclipse.appengine.login;

import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.core.runtime.IPath;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;

/**
 * Provides helper methods to save and retrieve an OAuth refresh token obtained via using Google OAuth service
 *
 * @see GoogleLoginService
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
   * @throws IOException if the JSON file with the credential cannot be created
   */
  public void saveCredential(IPath workDirectory, Credential credential) throws IOException {
    String jsonCredential = credentialHelper.toJson(credential);
    Files.write(workDirectory.append(CREDENTIAL_FILENAME).toFile().toPath(), 
                jsonCredential.getBytes(Charsets.UTF_8));
  }
  
  /**
   * Returns the path to the JSON file containing the OAuth credentials.
   * <p>
   * The method will not check the validity of the provided path and will not check whether the JSON file exists in the
   * returned location.
   *
   * @param directory contains the OAuth JSON file
   * @return the path to the JSON file with the OAuth credentials
   */
  public static IPath getCredentialFilePath(IPath directory) {
    return directory.append(CREDENTIAL_FILENAME);
  }
}
