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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.preferences.AnalyticsPreferences;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager.PingEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.widgets.Display;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsPingManagerTest {

  @Mock private IEclipsePreferences preferences;
  @Mock private Display display;
  @Mock private ConcurrentLinkedQueue<PingEvent> pingEventQueue;

  private AnalyticsPingManager pingManager;

  @Before
  public void setUp() {
    // Pretend ping event queue is always empty to prevent making actual HTTP requests.
    when(pingEventQueue.isEmpty()).thenReturn(true);

    pingManager = new AnalyticsPingManager("https://non-null-url-to-enable-mananger",
        "clientId", preferences, display, pingEventQueue);
  }

  @Test
  public void testEventTypeEventNameConvention() {
    PingEvent event = new PingEvent("some.event-name", null, null, null);
    Map<String, String> parameters = AnalyticsPingManager.buildParametersMap("clientId", event);
    assertEquals("/virtual/gcloud-eclipse-tools/some.event-name", parameters.get("dp"));
  }

  @Test
  public void testVirtualHostSet() {
    PingEvent event = new PingEvent("some.event-name", null, null, null);
    Map<String, String> parameters = AnalyticsPingManager.buildParametersMap("clientId", event);
    assertTrue(parameters.get("dh").startsWith("virtual."));
  }

  @Test
  public void testMetadataConvention() {
    PingEvent event = new PingEvent("some.event-name", "times-happened", "1234", null);
    Map<String, String> parameters = AnalyticsPingManager.buildParametersMap("clientId", event);
    assertEquals("times-happened=1234", parameters.get("dt"));
  }

  @Test
  public void testClientId() {
    PingEvent event = new PingEvent("some.event-name", null, null, null);
    Map<String, String> parameters = AnalyticsPingManager.buildParametersMap("clientId", event);
    assertEquals("clientId", parameters.get("cid"));
  }

  @Test
  public void testOptInDialogShown_optInNotRegisteredAndNotYetOptedIn() {
    mockOptIn(false);
    mockOptInRegistered(false);
    verifyOptInDialogOpen(times(1));
  }

  @Test
  public void testOptInDialogSkipped_optInNotRegisteredAndAlreadyOptedIn() {
    mockOptIn(true);
    mockOptInRegistered(false);
    verifyOptInDialogOpen(never());
  }

  @Test
  public void testOptInDialogSkipped_optInRegisteredAndNotYetOptedIn() {
    mockOptIn(false);
    mockOptInRegistered(true);
    verifyOptInDialogOpen(never());
  }

  @Test
  public void testOptInDialogSkipped_optInRegisteredAndAlreadyOptedIn() {
    mockOptIn(true);
    mockOptInRegistered(true);
    verifyOptInDialogOpen(never());
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

  private void verifyOptInDialogOpen(VerificationMode verificationMode) {
    pingManager.showOptInDialogIfNeeded(null);
    verify(display, verificationMode).syncExec(any(Runnable.class));
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
  public void testSendPingArguments_validArgumentsDoNotThrowException() {
    pingManager.sendPing("eventName", "metadataKey", "metadataValue");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSendPingArguments_nullEventName() {
    pingManager.sendPing(null, "metadataKey", "metadataValue");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSendPingArguments_emptyEventName() {
    pingManager.sendPing("", "metadataKey", "metadataValue");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSendPingArguments_nullMetadataKeyWithNonNullValue() {
    pingManager.sendPing("eventName", null, "metadataValue");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSendPingArguments_emptyMetadataKeyWithNonNullValue() {
    pingManager.sendPing("eventName", "", "metadataValue");
  }

  @Test
  public void testSendPingArguments_nullMetadataValueDoNotThrowException() {
    pingManager.sendPing("eventName", "metadataKey", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSendPingArguments_emptyMetadataValue() {
    pingManager.sendPing("eventName", "metadataKey", "");
  }
}
