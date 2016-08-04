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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.ide.login.OAuthData;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TransientOAuthDataStoreTest {

  @Mock private IEclipseContext eclipseContext;

  @Test
  public void testLoadOAuthData_emptyStoreReturnsNonNullOAuthData() {
    when(eclipseContext.get(anyString())).thenReturn(null);

    OAuthData oAuthData = new TransientOAuthDataStore(eclipseContext).loadOAuthData();
    Assert.assertNotNull(oAuthData);
    Assert.assertEquals(null, oAuthData.getAccessToken());
    Assert.assertEquals(null, oAuthData.getRefreshToken());
    Assert.assertEquals(null, oAuthData.getStoredEmail());
    Assert.assertEquals(0, oAuthData.getAccessTokenExpiryTime());
  }

  @Test
  public void testSaveAndLoadOAuthData() {
    OAuthData inputData = mock(OAuthData.class);
    TransientOAuthDataStore dataStore = new TransientOAuthDataStore(eclipseContext);
    dataStore.saveOAuthData(inputData);
    dataStore.loadOAuthData();

    ArgumentCaptor<OAuthData> argumentCaptor = ArgumentCaptor.forClass(OAuthData.class);
    verify(eclipseContext).set(anyString(), argumentCaptor.capture());
    verify(eclipseContext).get(anyString());
    Assert.assertEquals(inputData, argumentCaptor.getValue());
  }
}
