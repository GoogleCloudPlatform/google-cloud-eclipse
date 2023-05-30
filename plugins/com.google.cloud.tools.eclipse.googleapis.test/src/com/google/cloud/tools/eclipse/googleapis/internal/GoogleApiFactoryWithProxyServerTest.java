/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.googleapis.internal;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.appengine.v1.Appengine.Apps;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider;
import com.google.cloud.tools.eclipse.test.util.TestAccountProvider.State;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.net.URI;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("DoNotMock")
@RunWith(MockitoJUnitRunner.class)
public class GoogleApiFactoryWithProxyServerTest {

  @Mock private JsonFactory jsonFactory;
  @Mock private IProxyService proxyService;
  @Mock private Credential credential;
  @Mock private LoadingCache<GoogleApi, HttpTransport> transportCache;
  @Captor private ArgumentCaptor<IProxyChangeListener> proxyListenerCaptor =
      ArgumentCaptor.forClass(IProxyChangeListener.class);

  private GoogleApiFactory googleApiFactory;

  @Before
  public void setUp() {
    TestAccountProvider.setAsDefaultProvider(State.LOGGED_IN);
    googleApiFactory = new GoogleApiFactory();
    when(transportCache.getUnchecked(any(GoogleApi.class)))
        .thenReturn(mock(HttpTransport.class));
    googleApiFactory.setTransportCache(transportCache);
    assertTrue(googleApiFactory.getCredential().isPresent());
  }

  @Test
  public void testNewAppsApi_userAgentIsSet() throws IOException {
    Apps api = googleApiFactory.newAppsApi();
    assertThat(api.get("").getRequestHeaders().getUserAgent(),
               containsString(CloudToolsInfo.USER_AGENT));
  }

  @Test
  public void testNewProjectsApi_userAgentIsSet() throws IOException {
    Projects api = googleApiFactory.newProjectsApi();
    assertThat(api.get("").getRequestHeaders().getUserAgent(),
               containsString(CloudToolsInfo.USER_AGENT));
  }

  @Test
  public void testInit_ProxyChangeHandlerIsSet() {
    when(proxyService.select(any(URI.class))).thenReturn(new IProxyData[0]);
    googleApiFactory.setProxyService(proxyService);

    verify(proxyService).addProxyChangeListener(any(IProxyChangeListener.class));
  }
}
