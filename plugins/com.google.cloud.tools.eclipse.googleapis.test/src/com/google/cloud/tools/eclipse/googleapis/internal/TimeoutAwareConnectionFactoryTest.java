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

import com.google.cloud.tools.eclipse.test.util.http.TestHttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;

public class TimeoutAwareConnectionFactoryTest {

  private static final String NONEXISTENTDOMAIN =
      "http://nonexistentdomain" + new Random(System.currentTimeMillis()).nextLong() + ".com";
  private static final String PROXY_RESPONSE = "Proxy connected to " + NONEXISTENTDOMAIN;

  @Rule public TestHttpServer proxyServer = new TestHttpServer("", PROXY_RESPONSE);

  @Test
  public void testConstructorWithTimeouts() throws MalformedURLException, IOException {
    HttpURLConnection connection =
        new TimeoutAwareConnectionFactory(1764, 3528)
            .openConnection(new URL(proxyServer.getAddress()));
    assertThat(connection.getConnectTimeout(), is(1764));
    assertThat(connection.getReadTimeout(), is(3528));
    connectReadAndDisconnect(connection);
  }

  @Test
  public void testConstructorWithProxy() throws MalformedURLException, IOException {
    Proxy proxy =
        new Proxy(Type.HTTP,
                  new InetSocketAddress(proxyServer.getHostname(), proxyServer.getPort()));
    HttpURLConnection connection =
        new TimeoutAwareConnectionFactory(proxy).openConnection(new URL(NONEXISTENTDOMAIN));
    connectReadAndDisconnect(connection);
  }

  @Test
  public void testConstructorWithProxyAndTimeouts() throws MalformedURLException, IOException {
    Proxy proxy =
        new Proxy(Type.HTTP,
                  new InetSocketAddress(proxyServer.getHostname(), proxyServer.getPort()));
    HttpURLConnection connection =
        new TimeoutAwareConnectionFactory(proxy, 1764, 3528)
            .openConnection(new URL(NONEXISTENTDOMAIN));
    connectReadAndDisconnect(connection);
    assertThat(connection.getConnectTimeout(), is(1764));
    assertThat(connection.getReadTimeout(), is(3528));
  }

  private void connectReadAndDisconnect(HttpURLConnection connection) throws IOException {
    connection.connect();
    assertThat(connection.getResponseCode(), is(200));
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
      assertThat(reader.readLine(), is(PROXY_RESPONSE));
    } finally {
      connection.disconnect();
    }
  }
}
