package com.google.cloud.tools.eclipse.appengine.login;

import org.junit.Assert;
import org.junit.Test;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.gson.Gson;

public class CredentialHelperTest {

  @Test
  public void testGetJsonCredential() {
    Credential credential = createCredential("fake_access_token", "fake_refresh_token");
    String jsonCredential = new CredentialHelper().toJson(credential);

    CredentialType credentialType = new Gson().fromJson(jsonCredential, CredentialType.class);
    Assert.assertEquals(credentialType.client_id, Constants.getOAuthClientId());
    Assert.assertEquals(credentialType.client_secret, Constants.getOAuthClientSecret());
    Assert.assertEquals(credentialType.refresh_token, "fake_refresh_token");
    Assert.assertEquals(credentialType.type, "authorized_user");
  }

  private class CredentialType {
    private String client_id;
    private String client_secret;
    private String refresh_token;
    private String type;
  };

  private Credential createCredential(String accessToken, String refreshToken) {
    GoogleCredential credential = new GoogleCredential.Builder()
        .setTransport(new NetHttpTransport())
        .setJsonFactory(new JacksonFactory())
        .setClientSecrets(Constants.getOAuthClientId(), Constants.getOAuthClientSecret())
        .build();
    credential.setAccessToken(accessToken);
    credential.setRefreshToken(refreshToken);
    return credential;
  }
}
