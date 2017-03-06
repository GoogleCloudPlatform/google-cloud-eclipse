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

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.ConnectionFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.cache.CacheLoader;

class TransportCacheLoader extends CacheLoader<GoogleApiUrl, HttpTransport> {
  
  private final ProxyFactory proxyFactory;

  TransportCacheLoader(ProxyFactory proxyFactory) {
    this.proxyFactory = proxyFactory;
  }

  @Override
  public HttpTransport load(GoogleApiUrl url) throws Exception {
    ConnectionFactory connectionFactory =
        new TimeoutAwareConnectionFactory(proxyFactory.createProxy(url.getUrl()));
    return new NetHttpTransport.Builder().setConnectionFactory(connectionFactory).build();
  }
}