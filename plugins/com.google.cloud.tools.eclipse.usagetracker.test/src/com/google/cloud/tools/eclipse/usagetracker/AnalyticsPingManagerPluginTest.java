/*
 * Copyright 2016 Google Inc.
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

import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager.PingEvent;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsPingManagerPluginTest {

  @Mock private IEclipsePreferences preferences;
  @Mock private ConcurrentLinkedQueue<PingEvent> pingEventQueue;

  private AnalyticsPingManager pingManager;

  @Before
  public void setUp() {
    // Pretend ping event queue is always empty to prevent making actual HTTP requests.
    when(pingEventQueue.isEmpty()).thenReturn(true);
    when(preferences.get("ANALYTICS_CLIENT_ID", null)).thenReturn("clientId");

    pingManager = new AnalyticsPingManager("https://example.com", preferences, pingEventQueue);
  }
  
  @Test
  public void testBuildJson() {
    Gson gson = new Gson();
    ImmutableMap<String, String> metadata = ImmutableMap.of(
        "foo", "bar",
        "bax", "bat");
    PingEvent event = new PingEvent("SomeEvent", metadata, null);
    String json = pingManager.jsonEncode(event);

    Type mapType = new TypeToken<Map<String, ?>>(){}.getType();
    Map<String, ?> root = gson.fromJson(json, mapType);

    Map<String, ?> clientInfo = (Map<String, ?>) root.get("client_info");
    Assert.assertEquals("DESKTOP", clientInfo.get("client_type"));  
    Assert.assertEquals("CONCORD", root.get("log_source"));

    long requestTimeMs = ((Double) root.get("request_time_ms")).longValue();
    Assert.assertTrue(requestTimeMs >= 1000000);
    
    Map<String, String> desktopClientInfo =
        (Map<String, String>) clientInfo.get("desktop_client_info");
    Assert.assertTrue(desktopClientInfo.get("os").length() > 1);

    List<Map<String, Object>> logEvents = (List<Map<String, Object>>) root.get("log_event");
    Assert.assertEquals(1, logEvents.size());

    Map<String, Object> logEvent = logEvents.get(0);
    long eventTimeMs = ((Double) logEvent.get("event_time_ms")).longValue();
    Assert.assertTrue(eventTimeMs >= 1000000);

    String sourceExtensionJson = (String) logEvent.get("source_extension_json");

    // double encoded
    Type sourceExtensionJsonType = new TypeToken<Map<String, ?>>(){}.getType();
    Map<String, ?> source = gson.fromJson(sourceExtensionJson, sourceExtensionJsonType);
    Assert.assertEquals("CLOUD_TOOLS_FOR_ECLIPSE", source.get("console_type"));
    Assert.assertEquals("clientId", source.get("client_install_id"));
    Assert.assertEquals("SomeEvent", source.get("event_name"));

    List<Map<String, String>> eventMetadata =
        (List<Map<String, String>>) source.get("event_metadata");
    Assert.assertEquals(4, eventMetadata.size());

    Assert.assertEquals("foo", eventMetadata.get(0).get("key"));
    Assert.assertEquals("bar", eventMetadata.get(0).get("value"));

    Assert.assertEquals("bax", eventMetadata.get(1).get("key"));
    Assert.assertEquals("bat", eventMetadata.get(1).get("value"));

    Assert.assertEquals("ct4e-version", eventMetadata.get(2).get("key"));
    Assert.assertEquals("0.0.0", eventMetadata.get(2).get("value"));

    // expected value depends on host
    Assert.assertEquals("eclipse-version", eventMetadata.get(3).get("key"));
    Assert.assertNotNull(eventMetadata.get(3).get("value"));
  }

}
