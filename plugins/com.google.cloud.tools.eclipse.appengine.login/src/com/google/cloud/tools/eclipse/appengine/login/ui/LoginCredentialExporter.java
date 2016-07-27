package com.google.cloud.tools.eclipse.appengine.login.ui;

import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.window.IShellProvider;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.eclipse.appengine.login.CredentialHelper;
import com.google.cloud.tools.eclipse.appengine.login.GoogleLoginService;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;

public class LoginCredentialExporter {

  private static final String CREDENTIAL_FILENAME = "gcloud-credentials.json";
  
  private GoogleLoginService loginService;
  private CredentialHelper credentialHelper;
  
  public LoginCredentialExporter() {
    this(new GoogleLoginService(), new CredentialHelper());
  }
  
  @VisibleForTesting
  LoginCredentialExporter(GoogleLoginService googleLoginService, CredentialHelper credentialHelper) {
    this.loginService = googleLoginService;
    this.credentialHelper = credentialHelper;
  }

  public void logInAndSaveCredential(IPath workDirectory, IShellProvider shellProvider) throws IOException, 
                                                                                               CoreException {
    Credential credential = loginService.getActiveCredential(shellProvider);
    if (credential == null) {
      throw new CoreException(StatusUtil.error(LoginCredentialExporter.class, "Login failed"));
    }

    String jsonCredential = credentialHelper.toJson(credential);
    Files.write(workDirectory.append(CREDENTIAL_FILENAME).toFile().toPath(), 
                jsonCredential.getBytes(Charsets.UTF_8));
  }
  
  public static IPath getCredentialFilePath(IPath directory) {
    return directory.append(CREDENTIAL_FILENAME);
  }
}
