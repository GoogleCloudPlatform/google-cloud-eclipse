/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.ui.status;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import com.google.cloud.tools.eclipse.ui.status.Incident.Severity;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;

public class IncidentTest {

  /** Verify Gson correctly reads in an incident detail and skips unnecessary bits. */
  @Test
  public void testSingleCompletedIncident() {
    String json =
        "{\n"
            + "    \"begin\": \"2018-01-25T20:21:00Z\",\n"
            + "    \"created\": \"2018-01-25T20:21:56Z\",\n"
            + "    \"end\": \"2018-01-25T21:18:23Z\",\n"
            + "    \"external_desc\": \"We are investigating an issue with Google Cloud Networking. We will provide more information by 13:00 US/Pacific.\",\n"
            + "    \"modified\": \"2018-01-25T21:18:23Z\",\n"
            + "    \"most-recent-update\": {\n"
            + "      \"created\": \"2018-01-25T21:18:23Z\",\n"
            + "      \"modified\": \"2018-01-25T21:18:23Z\",\n"
            + "      \"text\": \"The issue with Google Cloud Networking has been resolved for all affected users as of 2018-01-25 13:15 US/Pacific. We will conduct an internal investigation of this issue and make appropriate improvements to our systems to help prevent or minimize future recurrence. We will provide a more detailed analysis of this incident once we have completed our internal investigation.\",\n"
            + "      \"when\": \"2018-01-25T21:18:23Z\"\n"
            + "    },\n"
            + "    \"number\": 18004,\n"
            + "    \"public\": true,\n"
            + "    \"service_key\": \"cloud-networking\",\n"
            + "    \"service_name\": \"Google Cloud Networking\",\n"
            + "    \"severity\": \"medium\",\n"
            + "    \"updates\": [\n"
            + "      {\n"
            + "        \"created\": \"2018-01-25T21:18:23Z\",\n"
            + "        \"modified\": \"2018-01-25T21:18:23Z\",\n"
            + "        \"text\": \"The issue with Google Cloud Networking has been resolved for all affected users as of 2018-01-25 13:15 US/Pacific. We will conduct an internal investigation of this issue and make appropriate improvements to our systems to help prevent or minimize future recurrence. We will provide a more detailed analysis of this incident once we have completed our internal investigation.\",\n"
            + "        \"when\": \"2018-01-25T21:18:23Z\"\n"
            + "      },\n"
            + "      {\n"
            + "        \"created\": \"2018-01-25T21:11:16Z\",\n"
            + "        \"modified\": \"2018-01-25T21:11:16Z\",\n"
            + "        \"text\": \"The issue with Google Cloud Networking should be resolved for the majority of users and we expect a full resolution in the near future. We will provide another status update by 2018-01-25 14:00 US/Pacific with current details.\",\n"
            + "        \"when\": \"2018-01-25T21:11:16Z\"\n"
            + "      },\n"
            + "      {\n"
            + "        \"created\": \"2018-01-25T20:21:56Z\",\n"
            + "        \"modified\": \"2018-01-25T20:21:56Z\",\n"
            + "        \"text\": \"We are investigating an issue with Google Cloud Networking. We will provide more information by 13:00 US/Pacific.\",\n"
            + "        \"when\": \"2018-01-25T20:21:56Z\"\n"
            + "      }\n"
            + "    ],\n"
            + "    \"uri\": \"/incident/cloud-networking/18004\"\n"
            + "  }";
    Gson gson = new Gson();
    Incident incident = gson.fromJson(json, Incident.class);
    assertEquals(18004, incident.id);
    assertNotNull(incident.begin);
    assertEquals(1516911660000L, incident.begin.getTime());
    assertNotNull(incident.end);
    assertEquals(1516915103000L, incident.end.getTime());
    assertEquals(Severity.MEDIUM, incident.severity);
    assertEquals("cloud-networking", incident.serviceKey);
    assertEquals("Google Cloud Networking", incident.serviceName);
    assertEquals("/incident/cloud-networking/18004", incident.uri);
    assertEquals(
        "We are investigating an issue with Google Cloud Networking. We will provide more information by 13:00 US/Pacific.",
        incident.description);
  }

  /** Verify Gson correctly reads in an incident detail that is in progress. */
  @Test
  public void testSingleInProgressIncident() {
    String json =
        "{\n"
            + "    \"begin\": \"2018-01-25T20:21:00Z\",\n"
            + "    \"created\": \"2018-01-25T20:21:56Z\",\n"
            + "    \"external_desc\": \"We are investigating an issue with Google Cloud Networking. We will provide more information by 13:00 US/Pacific.\",\n"
            + "    \"number\": 18004,\n"
            + "    \"public\": true,\n"
            + "    \"service_key\": \"cloud-networking\",\n"
            + "    \"service_name\": \"Google Cloud Networking\",\n"
            + "    \"severity\": \"medium\",\n"
            + "    \"uri\": \"/incident/cloud-networking/18004\"\n"
            + "  }";
    Gson gson = new Gson();
    Incident incident = gson.fromJson(json, Incident.class);
    assertEquals(18004, incident.id);
    assertNotNull(incident.begin);
    assertEquals(1516911660000L, incident.begin.getTime());
    assertNull(incident.end);
    assertEquals(Severity.MEDIUM, incident.severity);
    assertEquals("cloud-networking", incident.serviceKey);
    assertEquals("Google Cloud Networking", incident.serviceName);
    assertEquals("/incident/cloud-networking/18004", incident.uri);
    assertEquals(
        "We are investigating an issue with Google Cloud Networking. We will provide more information by 13:00 US/Pacific.",
        incident.description);
  }

  @Test
  public void testToString() {
    Incident incident = new Incident();
    incident.id = 1;
    incident.severity = Severity.LOW;
    incident.serviceKey = "service-key";
    incident.serviceName = "Service Name";
    incident.uri = "/incident/1";
    incident.description = "It is snack time";
    assertEquals("Incident 1 [LOW, Service Name]: It is snack time", incident.toString());
  }

  @Test
  public void testGetHighestSeverity() {
    Incident low = new Incident();
    low.severity = Severity.LOW;
    Incident medium = new Incident();
    medium.severity = Severity.MEDIUM;
    Incident high = new Incident();
    high.severity = Severity.HIGH;

    assertEquals(Severity.LOW, Incident.getHighestSeverity(Arrays.asList(low)));
    assertEquals(Severity.MEDIUM, Incident.getHighestSeverity(Arrays.asList(low, medium)));
    assertEquals(Severity.HIGH, Incident.getHighestSeverity(Arrays.asList(low, medium, high)));
  }

  @Test
  public void testGetAffectedServiceNames() {
    Incident one = new Incident();
    one.serviceName = "one";
    Incident two = new Incident();
    two.serviceName = "two";

    Collection<String> justOne = Incident.getAffectedServiceNames(Arrays.asList(one));
    assertThat(justOne, hasSize(1));
    assertThat(justOne, hasItem("one"));
    Collection<String> oneAndTwo = Incident.getAffectedServiceNames(Arrays.asList(one, two));
    assertThat(oneAndTwo, hasSize(2));
    assertThat(oneAndTwo, hasItem("one"));
    assertThat(oneAndTwo, hasItem("two"));
  }
}
