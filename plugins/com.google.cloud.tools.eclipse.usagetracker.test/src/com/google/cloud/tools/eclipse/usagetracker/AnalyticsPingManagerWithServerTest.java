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
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.test.util.http.TestHttpServer;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager.PingEvent;
import com.google.cloud.tools.eclipse.util.CloudToolsInfo;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.ui.PlatformUI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsPingManagerWithServerTest {

  @Mock private IEclipsePreferences preferences;

  @Rule public TestHttpServer server = new TestHttpServer("", "");

  private AnalyticsPingManager pingManager;

  @Before
  public void setUp() {
    when(preferences.getBoolean(eq(AnalyticsPreferences.ANALYTICS_OPT_IN), anyBoolean()))
        .thenReturn(true);  // Simulate user has opted in.

    pingManager = new AnalyticsPingManager(
        server.getAddress(), "unique-client-id", preferences,
        PlatformUI.getWorkbench().getDisplay(), new ConcurrentLinkedQueue<PingEvent>());
  }

  @Test
  public void testSendPing_noMetadata() throws InterruptedException {
    pingManager.sendPing("some.event-name", null, null, null);
    pingManager.eventFlushJob.join();

    Map<String, String[]> parameters = server.getRequestParameters();
    verifyCommonParameters(parameters, "some.event-name");
    assertNull(parameters.get("dt"));
  }

  @Test
  public void testSendPing_withMetadata() throws InterruptedException {
    pingManager.sendPing("another.event-name", "times-happened", "1234", null);
    pingManager.eventFlushJob.join();

    Map<String, String[]> parameters = server.getRequestParameters();
    verifyCommonParameters(parameters, "another.event-name");
    assertEquals("times-happened=1234", parameters.get("dt")[0]);
  }

  private void verifyCommonParameters(Map<String, String[]> parameters, String expectedEventName) {
    assertEquals("1", parameters.get("v")[0]);
    assertEquals("pageview", parameters.get("t")[0]);
    assertEquals("unique-client-id", parameters.get("cid")[0]);
    assertEquals("/virtual/" + CloudToolsInfo.METRICS_NAME + "/" + expectedEventName,
        parameters.get("dp")[0]);
    assertEquals(CloudToolsInfo.METRICS_NAME, parameters.get("cd19")[0]);
    assertEquals(expectedEventName, parameters.get("cd20")[0]);
    assertEquals("virtual.eclipse", parameters.get("dh")[0]);
    assertEquals("1", parameters.get("cd21")[0]);
    assertEquals("0", parameters.get("cd16")[0]);
    assertEquals("0", parameters.get("cd17")[0]);
    assertEquals("0", parameters.get("ni")[0]);
    assertEquals("0", parameters.get("ni")[0]);
  }
}
