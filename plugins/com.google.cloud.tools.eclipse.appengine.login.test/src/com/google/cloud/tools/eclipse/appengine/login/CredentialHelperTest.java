package com.google.cloud.tools.eclipse.appengine.login;

import org.junit.Assert;
import org.junit.Test;

import com.google.api.client.auth.oauth2.Credential;
import com.google.gson.Gson;

public class CredentialHelperTest {

  @Test
  public void testCreateCredential() {
    Credential credential = new CredentialHelper().createCredential("fake_access_token", "fake_refresh_token");

    Assert.assertEquals(credential.getAccessToken(), "fake_access_token");
    Assert.assertEquals(credential.getRefreshToken(), "fake_refresh_token");
  }

  @Test
  public void testGetJsonCredential() {
    CredentialHelper credentialHelper = new CredentialHelper();
    Credential credential = credentialHelper.createCredential("fake_access_token", "fake_refresh_token");
    String jsonCredential = credentialHelper.toJson(credential);

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
}
