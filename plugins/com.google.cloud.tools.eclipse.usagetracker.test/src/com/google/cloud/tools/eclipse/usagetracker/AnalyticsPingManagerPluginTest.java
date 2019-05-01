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

import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager.PingEvent;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
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

    pingManager = new AnalyticsPingManager("https://non-null-url-to-enable-mananger",
        preferences, pingEventQueue);
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
