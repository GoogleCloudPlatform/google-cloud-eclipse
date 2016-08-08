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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.login.ui.LoginServiceUi;
import com.google.cloud.tools.ide.login.LoggerFacade;
import com.google.cloud.tools.ide.login.OAuthData;
import com.google.cloud.tools.ide.login.OAuthDataStore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

@RunWith(MockitoJUnitRunner.class)
public class GoogleLoginServiceTest {

  @Mock private OAuthDataStore dataStore;
  @Mock private OAuthData savedOAuthData;
  @Mock private LoginServiceUi uiFacade;
  @Mock private LoggerFacade loggerFacade;

  private static final SortedSet<String> OAUTH_SCOPES = Collections.unmodifiableSortedSet(
      new TreeSet<>(Arrays.asList(
          "email",
          "https://www.googleapis.com/auth/cloud-platform"
      )));

  @Before
  public void setUp() {
    when(dataStore.loadOAuthData()).thenReturn(savedOAuthData);
  }

  @Test
  public void testGoogleLoginService_clearSavedCredentialIfNullRefreshToken() {
    when(savedOAuthData.getRefreshToken()).thenReturn(null);

    GoogleLoginService loginService = new GoogleLoginService(dataStore, uiFacade, loggerFacade);
    Assert.assertNull(loginService.getCachedActiveCredential());
  }

  @Test
  public void testGoogleLoginService_clearSavedCredentialIfScopesChanged() {
    // Persisted credential in the data store has an out-dated scopes.
    SortedSet<String> newScope = new TreeSet<String>(Arrays.asList("new scope"));
    when(savedOAuthData.getStoredScopes()).thenReturn(newScope);
    when(savedOAuthData.getRefreshToken()).thenReturn("fake_refresh_token");

    GoogleLoginService loginService = new GoogleLoginService(dataStore, uiFacade, loggerFacade);
    Assert.assertNull(loginService.getCachedActiveCredential());
  }

  @Test
  public void testGoogleLoginService_restoreSavedCredential() {
    // Persisted credential in the data store is valid.
    when(savedOAuthData.getStoredScopes()).thenReturn(OAUTH_SCOPES);
    when(savedOAuthData.getRefreshToken()).thenReturn("fake_refresh_token");

    GoogleLoginService loginService = new GoogleLoginService(dataStore, uiFacade, loggerFacade);
    verify(dataStore, never()).clearStoredOAuthData();
    Assert.assertNotNull(loginService.getCachedActiveCredential());
  }
}
