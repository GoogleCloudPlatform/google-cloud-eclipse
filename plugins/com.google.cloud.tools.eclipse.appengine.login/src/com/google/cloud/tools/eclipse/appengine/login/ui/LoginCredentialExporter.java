package com.google.cloud.tools.eclipse.appengine.login.ui;

import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.window.IShellProvider;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.login.CredentialHelper;
import com.google.cloud.tools.eclipse.appengine.login.GoogleLoginService;
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
   * Retrieves the OAuth credentials via {@link GoogleLoginService#getActiveCredential(IShellProvider)} (which may
   * initiate a web-based login process if there is no active logged in user present) and saves the credentials to
   * a json file under <code>workDirectory</code>
   * <p>
   * <b>This method must be called from the UI thread.</b>
   *
   * @param workDirectory the destination for the json file containing the received OAuth credential
   * @param shellProvider used if a web-based logn process is needed
   * @throws IOException if communication with the login service fails or the json file with the credential
   * cannot be created
   * @throws CoreException if the login attempt was unsuccessful
   */
  public void saveCredential(IPath workDirectory, Credential credential) throws IOException {
    String jsonCredential = credentialHelper.toJson(credential);
    Files.write(workDirectory.append(CREDENTIAL_FILENAME).toFile().toPath(), 
                jsonCredential.getBytes(Charsets.UTF_8));
  }
  
  /**
   * Returns the path to the json file containing the OAuth credentials.
   * <p>
   * The method will not check the validity of the provided path and will not check whether the json file exists in the
   * returned location.
   *
   * @param directory contains the OAuth json file
   * @return the path to the json file with the OAuth credentials
   */
  public static IPath getCredentialFilePath(IPath directory) {
    return directory.append(CREDENTIAL_FILENAME);
  }
}
