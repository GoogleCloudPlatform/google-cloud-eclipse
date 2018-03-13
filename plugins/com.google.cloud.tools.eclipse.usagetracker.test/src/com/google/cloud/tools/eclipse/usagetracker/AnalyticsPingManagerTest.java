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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.mockito.verification.VerificationMode;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsPingManagerTest {

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
  public void testPingEventConstructor_nullEventName() {
    try {
      new PingEvent(null, EMPTY_MAP, null);
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("eventName null or empty", e.getMessage());
    }
  }

  @Test
  public void testPingEventConstructor_emptyEventName() {
    try {
      new PingEvent("", EMPTY_MAP, null);
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("eventName null or empty", e.getMessage());
    }
  }

  @Test
  public void testPingEventConstructor_nullMetadata() {
    try {
      new PingEvent("some.event-name", null, null);
      fail();
    } catch (NullPointerException e) {
      assertEquals("metadata is null", e.getMessage());
    }
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

  @Test
  public void testShouldShowOptInDialog_optInNotRegisteredAndNotYetOptedIn() {
    mockOptIn(false);
    mockOptInRegistered(false);
    assertTrue(pingManager.shouldShowOptInDialog());
  }

  @Test
  public void testShouldShowOptInDialog_optInNotRegisteredAndAlreadyOptedIn() {
    mockOptIn(true);
    mockOptInRegistered(false);
    assertFalse(pingManager.shouldShowOptInDialog());
  }

  @Test
  public void testShouldShowOptInDialog_optInRegisteredAndNotYetOptedIn() {
    mockOptIn(false);
    mockOptInRegistered(true);
    assertFalse(pingManager.shouldShowOptInDialog());
  }

  @Test
  public void testShouldShowOptInDialog_optInRegisteredAndAlreadyOptedIn() {
    mockOptIn(true);
    mockOptInRegistered(true);
    assertFalse(pingManager.shouldShowOptInDialog());
  }

  private void mockOptIn(boolean optIn) {
    when(preferences.getBoolean(eq(AnalyticsPreferences.ANALYTICS_OPT_IN), anyBoolean()))
        .thenReturn(optIn);
  }

  private void mockOptInRegistered(boolean registered) {
    when(preferences.getBoolean(eq(AnalyticsPreferences.ANALYTICS_OPT_IN_REGISTERED),
                                anyBoolean()))
        .thenReturn(registered);
  }

  @Test
  public void testSendPingScheduled_optInNotRegisteredAndNotYetOptedIn() {
    mockOptIn(false);
    mockOptInRegistered(false);
    verifyPingQueued(times(1));
  }

  @Test
  public void testSendPingScheduled_optInNotRegisteredAndAlreadyOptedIn() {
    mockOptIn(true);
    mockOptInRegistered(false);
    verifyPingQueued(times(1));
  }

  @Test
  public void testSendPingDiscarded_optInRegisteredAndNotYetOptedIn() {
    mockOptIn(false);
    mockOptInRegistered(true);
    verifyPingQueued(never());
  }

  @Test
  public void testSendPingScheduled_optInRegisteredAndAlreadyOptedIn() {
    mockOptIn(true);
    mockOptInRegistered(true);
    verifyPingQueued(times(1));
  }

  private void verifyPingQueued(VerificationMode verificationMode) {
    pingManager.sendPing("eventName", "metadataKey", "metadataValue");
    verify(pingEventQueue, verificationMode).add(any(PingEvent.class));
  }

  @Test
  public void testGetAnonymizedClientId_generateNewId() {
    when(preferences.get(eq(AnalyticsPreferences.ANALYTICS_CLIENT_ID), anyString()))
        .thenReturn(null);  // Simulate that client ID has never been generated.
    String clientId = AnalyticsPingManager.getAnonymizedClientId(preferences);
    assertFalse(clientId.isEmpty());
    verify(preferences).put(AnalyticsPreferences.ANALYTICS_CLIENT_ID, clientId);
  }

  @Test
  public void testGetAnonymizedClientId_useSavedId() {
    when(preferences.get(eq(AnalyticsPreferences.ANALYTICS_CLIENT_ID), anyString()))
        .thenReturn("some-unique-client-id");
    String clientId = AnalyticsPingManager.getAnonymizedClientId(preferences);
    assertEquals("some-unique-client-id", clientId);
    verify(preferences, never()).put(AnalyticsPreferences.ANALYTICS_CLIENT_ID, clientId);
  }

  @Test
  public void testSendPingArguments_validEventName() {
    pingManager.sendPing("eventName");
  }

  @Test
  public void testSendPingArguments_nullEventName() {
    try {
      pingManager.sendPing(null);
      fail();
   } catch (IllegalArgumentException e) {
      assertEquals("eventName null or empty", e.getMessage());
    }
  }

  @Test
  public void testSendPingArguments_emptyEventName() {
    try {
      pingManager.sendPing("");
      fail();
   } catch (IllegalArgumentException e) {
      assertEquals("eventName null or empty", e.getMessage());
    }
  }

  @Test
  public void testSendPingArguments_validMetadataKeyValue() {
    pingManager.sendPing("eventName", "metadataKey", "metadataValue");
  }

  @Test
  public void testSendPingArguments_nullMetadataKey() {
    try {
      pingManager.sendPing("eventName", null, "metadataValue");
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("metadataKey null or empty", e.getMessage());
    }
  }

  @Test
  public void testSendPingArguments_emptyMetadataKey() {
    try {
      pingManager.sendPing("eventName", "", "metadataValue");
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("metadataKey null or empty", e.getMessage());
    }
  }

  @Test
  public void testSendPingArguments_nullMetadataValue() {
    try {
      pingManager.sendPing("eventName", "metadataKey", null);
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("metadataValue null or empty", e.getMessage());
    }
  }

  @Test
  public void testSendPingArguments_emptyMetadataValue() {
    try {
      pingManager.sendPing("eventName", "metadataKey", "");
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("metadataValue null or empty", e.getMessage());
    }
  }

  @Test
  public void testSendPingArguments_validMetadataMap() {
    pingManager.sendPing("eventName", EMPTY_MAP);
  }
}
