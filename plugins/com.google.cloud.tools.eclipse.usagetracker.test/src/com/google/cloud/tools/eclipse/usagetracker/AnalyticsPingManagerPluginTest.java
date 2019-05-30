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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
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

  private static final ImmutableMap<String, String> EMPTY_MAP = ImmutableMap.of();

  @Mock private IEclipsePreferences preferences;
  @Mock private ConcurrentLinkedQueue<PingEvent> pingEventQueue;

  private AnalyticsPingManager pingManager;

  @Before
  public void setUp() {
    // Pretend ping event queue is always empty to prevent making actual HTTP requests.
    when(pingEventQueue.isEmpty()).thenReturn(true);
    when(preferences.get("ANALYTICS_CLIENT_ID", null)).thenReturn("clientId");

    pingManager = new AnalyticsPingManager("https://non-null-url-to-enable-manager", null,
        preferences, pingEventQueue);
  }
  
  @Test
  public void testBuildJson() {
    Gson gson = new Gson();
    ImmutableMap<String, String> metadata = ImmutableMap.of(
        "foo", "bar",
        "bax", "bat");
    PingEvent event = new PingEvent("SomeEvent", metadata, null);
    String json = pingManager.jsonEncode(event);

    Type singletonRootType = new TypeToken<List<Map<String, ?>>>(){}.getType();
    List<Map<String, ?>> singletonRoot = gson.fromJson(json, singletonRootType);
    Map<String, ?> root = singletonRoot.get(0);

    Map<String, ?> clientInfo = ((List<Map<String, ?>>) root.get("client_info")).get(0);
    Assert.assertEquals("DESKTOP", clientInfo.get("client_type"));  
    Assert.assertEquals("CONCORD", root.get("log_source_name"));  

    long requestTimeMs = ((Double) root.get("request_time_ms")).longValue();
    Assert.assertTrue(requestTimeMs >= 1000000);
    
    Map<String, String> desktopClientInfo =
        ((List<Map<String, String>>) clientInfo.get("desktop_client_info")).get(0);
    Assert.assertTrue(desktopClientInfo.get("os").length() > 1);

    List<Object> logEvents = ((List<List<Object>>) root.get("log_event")).get(0);
    Assert.assertEquals(1, logEvents.size());

    Map<String, Object> logEvent = (Map<String, Object>) logEvents.get(0);
    long eventTimeMs = ((Double) logEvent.get("event_time_ms")).longValue();
    Assert.assertTrue(eventTimeMs >= 1000000);

    String sourceExtensionJson = (String) logEvent.get("source_extension_json");

    // double encoded
    Type sourceExtensionJsonType = new TypeToken<Map<String, ?>>(){}.getType();
    Map<String, ?> source = gson.fromJson(sourceExtensionJson, sourceExtensionJsonType);
    Assert.assertEquals("CLOUD_TOOLS_FOR_ECLIPSE", source.get("console_type"));
    Assert.assertEquals("clientId", source.get("client_machine_id"));
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

  @Test
  public void testEventTypeEventNameConvention() {
    PingEvent event = new PingEvent("some.event-name", EMPTY_MAP, null);
    Map<String, String> parameters = pingManager.buildParametersMap(event);
    assertEquals("/virtual/gcloud-eclipse-tools/some.event-name", parameters.get("dp"));
  }

  @Test
  public void testVirtualHostSet() {
    PingEvent event = new PingEvent("some.event-name", EMPTY_MAP, null);
    Map<String, String> parameters = pingManager.buildParametersMap(event);
    assertThat(parameters.get("dh"), startsWith("virtual."));
  }

  @Test
  public void testMetadataConvention() {
    PingEvent event = new PingEvent("some.event-name",
        ImmutableMap.of("times-happened", "1234"), null);
    Map<String, String> parameters = pingManager.buildParametersMap(event);
    assertThat(parameters.get("dt"), containsString("times-happened=1234"));
  }

  @Test
  public void testMetadataConvention_multiplePairs() {
    PingEvent event = new PingEvent("some.event-name",
        ImmutableMap.of("times-happened", "1234", "mode", "debug"), null);
    Map<String, String> parameters = pingManager.buildParametersMap(event);
    assertThat(parameters.get("dt"), containsString("times-happened=1234"));
    assertThat(parameters.get("dt"), containsString("mode=debug"));
  }

  @Test
  public void testMetadataConvention_escaping() {
    PingEvent event = new PingEvent("some.event-name",
        ImmutableMap.of("key , \\ = k", "value , \\ = v"), null);
    Map<String, String> parameters = pingManager.buildParametersMap(event);
    assertThat(parameters.get("dt"), containsString("key \\, \\\\ \\= k=value \\, \\\\ \\= v"));
  }

  @Test
  public void testMetadataContainsPlatformInfo() {
    ImmutableMap<String, String> customMetadata = ImmutableMap.of("times-happened", "1234");
    PingEvent event = new PingEvent("some.event-name", customMetadata, null);
    Map<String, String> parameters = pingManager.buildParametersMap(event);
    assertThat(parameters.get("dt"), containsString("ct4e-version="));
    assertThat(parameters.get("dt"), containsString("eclipse-version="));
  }

  @Test
  public void testClientId() {
    PingEvent event = new PingEvent("some.event-name", EMPTY_MAP, null);
    Map<String, String> parameters = pingManager.buildParametersMap(event);
    assertEquals("clientId", parameters.get("cid"));
  }

}
