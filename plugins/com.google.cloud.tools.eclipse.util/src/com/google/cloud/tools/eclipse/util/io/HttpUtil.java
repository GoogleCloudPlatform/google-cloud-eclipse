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

package com.google.cloud.tools.eclipse.util.io;

import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.UrlEscapers;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpUtil {

  public static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;
  public static final int DEFAULT_READ_TIMEOUT_MS = 3000;

  public static void sendPost(String urlString, Map<String, String> parameters) throws IOException {
    String parametersString = getParametersString(parameters);

    HttpURLConnection connection = null;
    try {
      URL url = new URL(urlString);
      connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      // This prevent Analytics from identifying our pings as spam.
      connection.setRequestProperty("User-Agent", CloudToolsInfo.USER_AGENT);
      connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MS);
      connection.setReadTimeout(DEFAULT_READ_TIMEOUT_MS);
      byte[] bytesToWrite = parametersString.getBytes(StandardCharsets.UTF_8);
      connection.setFixedLengthStreamingMode(bytesToWrite.length);

      try (OutputStream out = connection.getOutputStream()) {
        out.write(bytesToWrite);
        out.flush();
      }
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  @VisibleForTesting
  static String getParametersString(Map<String, String> parametersMap) {
    StringBuilder resultBuilder = new StringBuilder();
    boolean ampersandNeeded = false;
    for (Map.Entry<String, String> entry : parametersMap.entrySet()) {
      if (ampersandNeeded) {
        resultBuilder.append('&');
      } else {
        ampersandNeeded = true;
      }
      resultBuilder.append(entry.getKey());
      resultBuilder.append('=');
      resultBuilder.append(UrlEscapers.urlFormParameterEscaper().escape(entry.getValue()));
    }
    return resultBuilder.toString();
  }
}
