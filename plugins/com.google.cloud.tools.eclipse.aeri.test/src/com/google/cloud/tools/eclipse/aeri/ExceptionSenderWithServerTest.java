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

package com.google.cloud.tools.eclipse.aeri;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.http.TestHttpServer;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import java.io.IOException;
import java.util.Map;
import org.eclipse.epp.logging.aeri.core.IThrowable;
import org.junit.Rule;
import org.junit.Test;

public class ExceptionSenderWithServerTest {

  @Rule public TestHttpServer server = new TestHttpServer("", "");

  @Test
  public void testSendException() throws IOException {
    IThrowable aeriThrowable = AeriTestUtil.getMockIThrowable(AeriTestUtil.sampleThrowable);

    new ExceptionSender(server.getAddress()).sendException(aeriThrowable,
        "eclipseBuildId Alpha", "javaVersion Bravo", "os Charie", "osVersion Delta",
        "userSeverity Echo", "userComment Foxtrot");

    assertEquals("POST", server.getRequestMethod());
    assertTrue(server.getRequestHeaders()
        .get("Content-Type").startsWith("multipart/form-data; boundary="));

    Map<String, String[]> parameters = server.getRequestParameters();

    assertEquals("CT4E", parameters.get("product")[0]); // from URL
    assertEquals("CT4E", parameters.get("product")[1]); // from POST body

    assertEquals(CloudToolsInfo.getToolsVersion(), parameters.get("version")[0]); // from URL
    assertEquals(CloudToolsInfo.getToolsVersion(), parameters.get("version")[1]); // from POST body

    assertEquals("eclipseBuildId: eclipseBuildId Alpha\n"
        + "javaVersion: javaVersion Bravo\nos: os Charie osVersion Delta\n"
        + "userSeverity: userSeverity Echo\nuserComment: userComment Foxtrot",
        parameters.get("comments")[0]);

    assertEquals("java.nio.charset.IllegalCharsetNameException: no CT4E packages in stack trace\n"
        + "\tat com.example.Charlie.boom(Charlie.java:34)\n"
        + "\tat com.example.Bob.callCharlie(Bob.java:30)\n"
        + "\tat com.example.Alice.callBob(Alice.java:26)\n"
        + "\tat java.lang.Thread.run(Thread.java:745)", parameters.get("exception_info")[0]);
  }
}
