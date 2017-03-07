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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.appengine.v1.Appengine.Apps;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.cloud.tools.eclipse.googleapis.GoogleApiException;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GoogleApiFactoryTest {

  @Mock private JsonFactory jsonFactory;
  @Mock private IProxyService proxyService;
  @Mock private Credential credential;
  @Mock LoadingCache<GoogleApiUrl, HttpTransport> transportCache;
  @Mock private ProxyFactory proxyFactory;
  @Captor private ArgumentCaptor<IProxyChangeListener> proxyChangeListenerCaptor =
      ArgumentCaptor.forClass(IProxyChangeListener.class);

  private GoogleApiFactory googleApiFactory;

  @Before
  public void setUp() throws ExecutionException {
    googleApiFactory = new GoogleApiFactory(proxyFactory);
    when(transportCache.get(any(GoogleApiUrl.class))).thenReturn(mock(HttpTransport.class));
    googleApiFactory.setTransportCache(transportCache);
  }

  @Test
  public void testNewAppsApi_userAgentIsSet() throws IOException, GoogleApiException {
    Apps api = googleApiFactory.newAppsApi(mock(Credential.class));
    assertThat(api.get("").getRequestHeaders().getUserAgent(),
               containsString(CloudToolsInfo.USER_AGENT));
  }

  @Test(expected = GoogleApiException.class)
  public void testNewAppsApi_transportCacheErrorTranslatedToGoogleApiException()
      throws ExecutionException, GoogleApiException {
    when(transportCache.get(any(GoogleApiUrl.class)))
        .thenThrow(new ExecutionException(new Exception("test")));
    googleApiFactory.newAppsApi(mock(Credential.class));
  }

  @Test
  public void testNewProjectsApi_userAgentIsSet() throws IOException, GoogleApiException {
    Projects api = googleApiFactory.newProjectsApi(mock(Credential.class));
    assertThat(api.get("").getRequestHeaders().getUserAgent(),
               containsString(CloudToolsInfo.USER_AGENT));
  }

  @Test(expected = GoogleApiException.class)
  public void testNewProjectsApi_transportCacheErrorTranslatedToGoogleApiException()
      throws ExecutionException, GoogleApiException {
    when(transportCache.get(any(GoogleApiUrl.class)))
        .thenThrow(new ExecutionException(new Exception("test")));
    googleApiFactory.newProjectsApi(mock(Credential.class));
  }

  @Test
  public void testSetProxyService() {
    googleApiFactory.setProxyService(proxyService);

    verify(proxyService).addProxyChangeListener(any(IProxyChangeListener.class));
    verify(proxyFactory).setProxyService(proxyService);
    verify(transportCache).invalidateAll();
  }

  @Test
  public void testUnsetDifferentProxyService() {
    googleApiFactory.setProxyService(proxyService);
    googleApiFactory.unsetProxyService(mock(IProxyService.class));

    verify(proxyService, never()).removeProxyChangeListener(any(IProxyChangeListener.class));
    verify(proxyFactory, never()).setProxyService(null);
    // called once when setProxyService() is called
    verify(transportCache).invalidateAll();
  }

  @Test
  public void testSetAndUnsetProxyService() {
    googleApiFactory.setProxyService(proxyService);
    googleApiFactory.unsetProxyService(proxyService);

    verify(proxyService).addProxyChangeListener(any(IProxyChangeListener.class));
    verify(proxyService).removeProxyChangeListener(any(IProxyChangeListener.class));
    verify(proxyFactory).setProxyService(proxyService);
    verify(proxyFactory).setProxyService(null);
    verify(transportCache, times(2)).invalidateAll();
  }

  @Test
  public void testProxyChangeListenerInvalidatesCache() {
    doNothing().when(proxyService).addProxyChangeListener(proxyChangeListenerCaptor.capture());
    googleApiFactory.setProxyService(proxyService);
    verify(transportCache).invalidateAll();

    proxyChangeListenerCaptor.getValue().proxyInfoChanged(mock(IProxyChangeEvent.class));
    verify(transportCache, times(2)).invalidateAll();
  }
}
