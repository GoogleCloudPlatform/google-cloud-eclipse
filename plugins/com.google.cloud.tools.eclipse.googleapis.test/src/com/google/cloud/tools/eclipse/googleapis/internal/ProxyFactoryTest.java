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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProxyFactoryTest {

  private static final String PROXY_HOST = "proxy.example.com";
  private static final int PROXY_PORT = 3128;

  @Mock private IProxyService proxyService;

  private ProxyFactory proxyFactory;

  @Before
  public void setUp() throws Exception {
    proxyFactory = new ProxyFactory();
  }

  @Test(expected = NullPointerException.class)
  public void testCreateProxy_nullArgument() throws URISyntaxException {
    proxyFactory.createProxy(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateProxy_httpSchemeIsUnsupported() throws URISyntaxException {
    proxyFactory.createProxy("http://exmaple.com");
  }

  @Test
  public void testCreateProxy_noProxyServiceCreatesDirectConnection() throws URISyntaxException {
    assertThat(proxyFactory.createProxy("https://exmaple.com").type(), is(Proxy.Type.DIRECT));
  }

  @Test
  public void testCreateProxy_noProxyDataCreatesDirectConnection() throws URISyntaxException {
    proxyFactory.setProxyService(proxyService);
    when(proxyService.select(any(URI.class))).thenReturn(new IProxyData[0]);
    assertThat(proxyFactory.createProxy("https://exmaple.com").type(), is(Proxy.Type.DIRECT));
  }

  @Test
  public void testCreateProxy_httpsProxyData() throws URISyntaxException {
    proxyFactory.setProxyService(proxyService);
    IProxyData proxyData = createProxyData(IProxyData.HTTPS_PROXY_TYPE, PROXY_HOST, PROXY_PORT);
    when(proxyService.select(any(URI.class))).thenReturn(new IProxyData[]{ proxyData });

    assertThat(proxyFactory.createProxy("https://exmaple.com").type(), is(Proxy.Type.HTTP));
  }

  @Test
  public void testCreateProxy_socksProxyData() throws URISyntaxException {
    proxyFactory.setProxyService(proxyService);
    IProxyData proxyData = createProxyData(IProxyData.SOCKS_PROXY_TYPE, PROXY_HOST, PROXY_PORT);
    when(proxyService.select(any(URI.class))).thenReturn(new IProxyData[]{ proxyData });

    assertThat(proxyFactory.createProxy("https://exmaple.com").type(), is(Proxy.Type.SOCKS));
  }

  @Test
  public void testCreateProxy_unsupportedProxyData() throws URISyntaxException {
    proxyFactory.setProxyService(proxyService);
    IProxyData proxyData = createProxyData(IProxyData.HTTP_PROXY_TYPE, PROXY_HOST, PROXY_PORT);
    when(proxyService.select(any(URI.class))).thenReturn(new IProxyData[]{ proxyData });

    assertThat(proxyFactory.createProxy("https://exmaple.com").type(), is(Proxy.Type.DIRECT));
  }

  @Test
  public void testCreateProxy_ifHttpsIsFirstItWillBeUsed() throws URISyntaxException {
    proxyFactory.setProxyService(proxyService);
    IProxyData httpsProxyData = createProxyData(IProxyData.HTTPS_PROXY_TYPE, PROXY_HOST, PROXY_PORT);
    IProxyData socksProxyData = createProxyData(IProxyData.SOCKS_PROXY_TYPE, PROXY_HOST, PROXY_PORT);
    when(proxyService.select(any(URI.class)))
        .thenReturn(new IProxyData[]{ httpsProxyData, socksProxyData });

    assertThat(proxyFactory.createProxy("https://exmaple.com").type(), is(Proxy.Type.HTTP));
  }

  @Test
  public void testCreateProxy_ifSocksIsFirstItWillBeUsed() throws URISyntaxException {
    proxyFactory.setProxyService(proxyService);
    IProxyData httpsProxyData = createProxyData(IProxyData.HTTPS_PROXY_TYPE, PROXY_HOST, PROXY_PORT);
    IProxyData socksProxyData = createProxyData(IProxyData.SOCKS_PROXY_TYPE, PROXY_HOST, PROXY_PORT);
    when(proxyService.select(any(URI.class)))
        .thenReturn(new IProxyData[]{ socksProxyData, httpsProxyData });

    assertThat(proxyFactory.createProxy("https://exmaple.com").type(), is(Proxy.Type.SOCKS));
  }

  @Test
  public void testCreateProxy_settingProxyServiceToNullCreatesDirectConnection()
      throws URISyntaxException {
    proxyFactory.setProxyService(proxyService);
    IProxyData proxyData = createProxyData(IProxyData.HTTPS_PROXY_TYPE, PROXY_HOST, PROXY_PORT);
    when(proxyService.select(any(URI.class))).thenReturn(new IProxyData[]{ proxyData });

    assertThat(proxyFactory.createProxy("https://exmaple.com").type(), is(Proxy.Type.HTTP));
    proxyFactory.setProxyService(null);
    assertThat(proxyFactory.createProxy("https://exmaple.com").type(), is(Proxy.Type.DIRECT));
  }

  private IProxyData createProxyData(String proxyType, String host, int port) {
    IProxyData proxyData = mock(IProxyData.class);
    when(proxyData.getType()).thenReturn(proxyType);
    when(proxyData.getHost()).thenReturn(host);
    when(proxyData.getPort()).thenReturn(port);
    return proxyData;
  }
}
