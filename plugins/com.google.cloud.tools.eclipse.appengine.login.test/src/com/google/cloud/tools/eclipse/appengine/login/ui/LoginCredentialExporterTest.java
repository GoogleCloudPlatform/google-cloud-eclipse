package com.google.cloud.tools.eclipse.appengine.login.ui;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.window.IShellProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.Credential.AccessMethod;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.cloud.tools.eclipse.appengine.login.GoogleLoginService;
import com.google.gson.Gson;

@RunWith(MockitoJUnitRunner.class)
public class LoginCredentialExporterTest {

  private static final String FAKE_REFRESH_TOKEN = "fake-refresh-token";
  @Mock private GoogleLoginService loginService;
  @Mock private IShellProvider shellProvider;
  @Mock private Credential credential;

  @Test(expected = CoreException.class)
  public void testLogInAndSaveCredential_exceptionOnFailedLogin() throws IOException, CoreException {
    when(loginService.getActiveCredential(shellProvider)).thenReturn(null);
    
    LoginCredentialExporter exporter = new LoginCredentialExporter(loginService);
    
    Path workDirectory = Files.createTempDirectory(null, new FileAttribute<?>[0]);
    workDirectory.toFile().deleteOnExit();
    IPath workDirectoryPath = new org.eclipse.core.runtime.Path(workDirectory.toString());
    exporter.logInAndSaveCredential(workDirectoryPath, shellProvider);
  }

  @Test
  public void testLogInAndSaveCredential() throws IOException, CoreException {
    Credential credential = new Credential.Builder(mock(AccessMethod.class))
        .setJsonFactory(mock(JsonFactory.class))
        .setTransport(mock(HttpTransport.class))
        .setClientAuthentication(mock(HttpExecuteInterceptor.class))
        .setTokenServerUrl(mock(GenericUrl.class))
        .build();
    credential.setRefreshToken(FAKE_REFRESH_TOKEN);
    when(loginService.getActiveCredential(shellProvider)).thenReturn(credential);
    
    LoginCredentialExporter exporter = new LoginCredentialExporter(loginService);
    
    Path workDirectory = Files.createTempDirectory(null, new FileAttribute<?>[0]);
    workDirectory.toFile().deleteOnExit();
    IPath workDirectoryPath = new org.eclipse.core.runtime.Path(workDirectory.toString());
    exporter.logInAndSaveCredential(workDirectoryPath, shellProvider);
    
    Credential exportedCredential = new Gson().fromJson(new FileReader(workDirectory.resolve("gcloud-credentials.json").toFile()), Credential.class);
    assertThat(exportedCredential.getRefreshToken(), is(FAKE_REFRESH_TOKEN));
  }

  @Test
  public void testGetCredentialFilePath() {
    fail("Not yet implemented");
  }

}
