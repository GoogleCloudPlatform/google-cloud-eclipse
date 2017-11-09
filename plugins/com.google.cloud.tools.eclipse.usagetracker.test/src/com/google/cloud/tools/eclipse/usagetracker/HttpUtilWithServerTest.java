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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.http.TestHttpServer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;

public class HttpUtilWithServerTest {

  @Rule public TestHttpServer server = new TestHttpServer("", "You're good!");

  private static final String LONG_PARAMETER = "Exception in thread \"main\""
      + " java.lang.IllegalArgumentException: Wrong argument!\n\tat B.main(B.java:73)";

  private static final Map<String, String> testParameters;
  static {
    testParameters = new HashMap<>();
    testParameters.put("dp", "/virtual/some-event-type/some-event-name");
    testParameters.put("exception_info", LONG_PARAMETER);
    testParameters.put("product", "CT4E");
  }

  @Test
  public void testSendPostMultipart() throws IOException {
    assertEquals(200, HttpUtil.sendPostMultipart(server.getAddress(), testParameters));
    assertTrue(server.getRequestHeaders().get("Content-Type")
        .startsWith("multipart/form-data; boundary="));

    verifyPostRequest();
  }

  @Test
  public void testSendPost() throws IOException {
    assertEquals(200, HttpUtil.sendPost(server.getAddress(), testParameters));
    assertEquals("application/x-www-form-urlencoded",
        server.getRequestHeaders().get("Content-Type"));

    verifyPostRequest();
  }

  private void verifyPostRequest() {
    assertEquals("POST", server.getRequestMethod());
    assertTrue(server.getRequestHeaders().get("User-Agent")
        .startsWith("gcloud-eclipse-tools/"));

    Map<String, String[]> parsedParameters = server.getRequestParameters();
    assertEquals("/virtual/some-event-type/some-event-name", parsedParameters.get("dp")[0]);
    assertEquals(LONG_PARAMETER, parsedParameters.get("exception_info")[0]);
    assertEquals("CT4E", parsedParameters.get("product")[0]);
  }
}
