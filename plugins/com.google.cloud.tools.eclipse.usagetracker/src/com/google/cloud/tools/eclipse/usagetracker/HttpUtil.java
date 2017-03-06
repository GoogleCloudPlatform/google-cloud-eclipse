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

package com.google.cloud.tools.eclipse.usagetracker;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.MultipartContent;
import com.google.api.client.http.javanet.NetHttpTransport;
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

  private static final String MULTIPART_BOUNDARY =
      "---------------------------45224ee4-f3c1-4b23-8df1-4012f722218c"; // some random UUID

  private static final HttpTransport transport = new NetHttpTransport();

  public static int sendPostMultipart(String urlString, Map<String, String> parameters)
      throws IOException {

    MultipartContent postBody = new MultipartContent()
        .setMediaType(new HttpMediaType("multipart/form-data"));
    postBody.setBoundary(MULTIPART_BOUNDARY);

    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      HttpContent partContent = ByteArrayContent.fromString(  // uses UTF-8 internally
          null /* part Content-Type */, entry.getValue());
      HttpHeaders partHeaders = new HttpHeaders()
          .set("Content-Disposition",  "form-data; name=\"" + entry.getKey() + "\"");

      postBody.addPart(new MultipartContent.Part(partHeaders, partContent));
    }

    GenericUrl url = new GenericUrl(new URL(urlString));
    HttpRequest request = transport.createRequestFactory().buildPostRequest(url, postBody);
    request.setHeaders(new HttpHeaders().setUserAgent(CloudToolsInfo.USER_AGENT));
    request.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MS);
    request.setReadTimeout(DEFAULT_READ_TIMEOUT_MS);

    HttpResponse response = request.execute();
    return response.getStatusCode();
  }

  public static int sendPost(String urlString, Map<String, String> parameters) throws IOException {
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
      return connection.getResponseCode();
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
