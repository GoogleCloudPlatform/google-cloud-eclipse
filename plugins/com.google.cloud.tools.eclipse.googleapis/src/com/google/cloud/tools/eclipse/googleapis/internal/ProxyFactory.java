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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

// needs to be public for Mockito
@VisibleForTesting
public class ProxyFactory {

  private static final Logger logger = Logger.getLogger(ProxyFactory.class.getName());

  private IProxyService proxyService;
  
  // needs to be public for Mockito
  @VisibleForTesting
  public Proxy createProxy(String url) throws URISyntaxException {
    Preconditions.checkNotNull(url, "url is null");
    Preconditions.checkArgument(!url.startsWith("http://"), "http is not a supported schema");
    
    IProxyService proxyServiceCopy = proxyService;
    if (proxyServiceCopy == null) {
      return Proxy.NO_PROXY;
    }

    IProxyData[] proxyDataForUri = proxyServiceCopy.select(new URI(url));
    for (final IProxyData iProxyData : proxyDataForUri) {
      switch (iProxyData.getType()) {
        case IProxyData.HTTPS_PROXY_TYPE:
          return new Proxy(Type.HTTP, new InetSocketAddress(iProxyData.getHost(),
                                                            iProxyData.getPort()));
        case IProxyData.SOCKS_PROXY_TYPE:
          return new Proxy(Type.SOCKS, new InetSocketAddress(iProxyData.getHost(),
                                                             iProxyData.getPort()));
        default:
          logger.warning("Unsupported proxy type: " + iProxyData.getType());
          break;
      }
    }
    return Proxy.NO_PROXY;
  }

  // needs to be public for Mockito
  @VisibleForTesting
  public void setProxyService(IProxyService proxyService) {
    this.proxyService = proxyService;
  }
}
