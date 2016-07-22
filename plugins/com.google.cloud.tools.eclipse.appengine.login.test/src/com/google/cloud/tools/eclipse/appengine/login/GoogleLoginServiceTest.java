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
import com.google.gson.Gson;

import org.junit.Assert;
import org.junit.Test;

public class GoogleLoginServiceTest {

  @Test
  public void testCreateCredential() {
    Credential credential = new GoogleLoginService().createCredentialHelper(
        "fake_access_token", "fake_refresh_token");

    Assert.assertEquals("fake_access_token", credential.getAccessToken());
    Assert.assertEquals("fake_refresh_token", credential.getRefreshToken());
  }

  @Test
  public void testGetJsonCredential() {
    String jsonCredential = GoogleLoginService.getJsonCredentialHelper("fake_refresh_token");

    CredentialType credentialType = new Gson().fromJson(jsonCredential, CredentialType.class);
    Assert.assertEquals(Constants.getOAuthClientId(), credentialType.client_id);
    Assert.assertEquals(Constants.getOAuthClientSecret(), credentialType.client_secret);
    Assert.assertEquals("fake_refresh_token", credentialType.refresh_token);
    Assert.assertEquals("authorized_user", credentialType.type);
  }

  private class CredentialType {
    private String client_id;
    private String client_secret;
    private String refresh_token;
    private String type;
  };
}
