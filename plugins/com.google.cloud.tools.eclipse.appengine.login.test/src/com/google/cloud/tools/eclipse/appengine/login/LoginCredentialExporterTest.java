package com.google.cloud.tools.eclipse.appengine.login;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.window.IShellProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.api.client.auth.oauth2.Credential;
import com.google.gson.Gson;

@RunWith(MockitoJUnitRunner.class)
public class LoginCredentialExporterTest {

  private static final String FAKE_REFRESH_TOKEN = "fake-refresh-token";
  private static final String FAKE_ACCESS_TOKEN = "fake-access-token";
  @Mock private GoogleLoginService loginService;
  @Mock private IShellProvider shellProvider;
  @Mock private Credential credential;
  private CredentialHelper credentialHelper = new CredentialHelper();

  @Test
  public void testSaveCredential_successful() throws IOException, CoreException {
    Path workDirectory = Files.createTempDirectory(null);
    workDirectory.toFile().deleteOnExit();
    IPath workDirectoryPath = new org.eclipse.core.runtime.Path(workDirectory.toString());

    Credential credential = new CredentialHelper().createCredential(FAKE_ACCESS_TOKEN, FAKE_REFRESH_TOKEN);
    LoginCredentialExporter exporter = new LoginCredentialExporter(credentialHelper);
    
    exporter.saveCredential(workDirectoryPath, credential);
    
    @SuppressWarnings("unchecked")
    Map<String, String> exportedCredential = new Gson().fromJson(new FileReader(workDirectory.resolve("gcloud-credentials.json").toFile()), HashMap.class);
    assertThat(exportedCredential.get("refresh_token"), is(FAKE_REFRESH_TOKEN));
  }
}
