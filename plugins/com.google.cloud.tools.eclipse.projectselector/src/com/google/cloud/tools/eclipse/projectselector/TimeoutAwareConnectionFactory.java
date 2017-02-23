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

package com.google.cloud.tools.eclipse.projectselector;

import com.google.api.client.http.javanet.DefaultConnectionFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Extends the {@link DefaultConnectionFactory} with option to specify connect and read timeout.
 *
 * TODO move this class into a separate API bundle
 * https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1438
 */
class TimeoutAwareConnectionFactory extends DefaultConnectionFactory {
  private final int connectTimeout;
  private final int readTimeout;

  TimeoutAwareConnectionFactory(int connectTimeout, int readTimeout) {
    super();
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
  }

  @Override
  public HttpURLConnection openConnection(URL url) throws IOException {
    HttpURLConnection connection = super.openConnection(url);
    connection.setConnectTimeout(connectTimeout);
    connection.setReadTimeout(readTimeout);
    return connection;
  }
}