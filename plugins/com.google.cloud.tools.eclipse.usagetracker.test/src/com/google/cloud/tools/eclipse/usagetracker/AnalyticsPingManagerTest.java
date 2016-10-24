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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.preferences.AnalyticsPreferences;
import com.google.cloud.tools.eclipse.usagetracker.AnalyticsPingManager.PingEvent;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.widgets.Display;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsPingManagerTest {

  private static final String UUID = "bee5d838-c3f8-4940-a944-b56973597e74";

  private static final String EVENT_TYPE = "some-event-type";
  private static final String EVENT_NAME = "some-event-name";

  private static final String VIRTUAL_DOCUMENT_PAGE =
      "/virtual/some-application/" + EVENT_TYPE + "/" + EVENT_NAME;

  private static final String METADATA_KEY = "some-custom-key";
  private static final String METADATA_VALUE = "some-custom-value";

  @SuppressWarnings("serial")
  private static final Map<String, String> RANDOM_PARAMETERS = Collections.unmodifiableMap(
      new HashMap<String, String>() {
        {
          put("v", "1");
          put("tid", "UA-12345678-1");
          put("ni", "0");
          put("t", "pageview");
          put("cd21", "1");
          put("cd16", "0");
          put("cd17", "0");
          put("cid", UUID);
          put("cd19", EVENT_TYPE);
          put("cd20", EVENT_NAME);
          put("dp", VIRTUAL_DOCUMENT_PAGE);
          put("dt", METADATA_KEY + "=" + METADATA_VALUE);
        }
      });

  @SuppressWarnings("serial")
  private static final Map<String, String> ENCODED_PARAMETERS = Collections.unmodifiableMap(
      new HashMap<String, String>() {
        {
          put("dt", "some-custom-key%3Dsome-custom-value");
          put("cd16", "0");
          put("cd17", "0");
          put("v", "1");
          put("t", "pageview");
          put("cd21", "1");
          put("cd20", "some-event-name");
          put("ni", "0");
          put("tid", "UA-12345678-1");
          put("dp", "%2Fvirtual%2Fsome-application%2Fsome-event-type%2Fsome-event-name");
          put("cid", "bee5d838-c3f8-4940-a944-b56973597e74");
          put("cd19", "some-event-type");
        }
      });

  @Mock private IEclipsePreferences preferences;
  @Mock private Display display;
  @Mock private ConcurrentLinkedQueue<PingEvent> pingEventQueue;

  @Test
  public void testGetParametersString() {
    String urlEncodedParameters = AnalyticsPingManager.getParametersString(RANDOM_PARAMETERS);

    String[] keyValuePairs = urlEncodedParameters.split("&");
    Assert.assertEquals(keyValuePairs.length, RANDOM_PARAMETERS.size());

    for (String pair : keyValuePairs) {
      String[] keyValue = pair.split("=");
      Assert.assertEquals(2, keyValue.length);
      Assert.assertEquals(keyValue[1], ENCODED_PARAMETERS.get(keyValue[0]));
    }
  }

  @Test
  public void testGetParametersString_percentEscaping() {
    Map<String, String> noEscape = new HashMap<>();
    noEscape.put("k", ".*-_abcXYZ");
    Assert.assertEquals("k=.*-_abcXYZ", AnalyticsPingManager.getParametersString(noEscape));

    Map<String, String> escape = new HashMap<>();
    escape.put("k", " ü한글+=,`~!@#$%^&()?<>{}][|:;/\\'\"");
    Assert.assertEquals("k=+%C3%BC%ED%95%9C%EA%B8%80%2B%3D%2C%60%7E%21%40%23"
        + "%24%25%5E%26%28%29%3F%3C%3E%7B%7D%5D%5B%7C%3A%3B%2F%5C%27%22",
        AnalyticsPingManager.getParametersString(escape));
  }

  @Test
  public void testEventTypeEventNameConvention() {
    PingEvent event = new PingEvent("some.event-name", null, null, null);
    Map<String, String> parameters = AnalyticsPingManager.buildParametersMap("clientId", event);
    Assert.assertEquals("/virtual/gcloud-eclipse-tools/some.event-name",
        parameters.get("dp"));
  }

  @Test
  public void testMetadataConvention() {
    PingEvent event = new PingEvent("some.event-name", "times-happened", "1234", null);
    Map<String, String> parameters = AnalyticsPingManager.buildParametersMap("clientId", event);
    Assert.assertEquals("times-happened=1234", parameters.get("dt"));
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
    AnalyticsPingManager pingManager =
        new AnalyticsPingManager(preferences, display, pingEventQueue, true);
    pingManager.unitTestMode = true;
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
    when(pingEventQueue.isEmpty()).thenReturn(true);
    AnalyticsPingManager pingManager =
        new AnalyticsPingManager(preferences, display, pingEventQueue, true);
    pingManager.unitTestMode = true;
    pingManager.sendPing("eventName", "metadataKey", "metadataValue");
    verify(pingEventQueue, verificationMode).add(any(PingEvent.class));
  }
}
