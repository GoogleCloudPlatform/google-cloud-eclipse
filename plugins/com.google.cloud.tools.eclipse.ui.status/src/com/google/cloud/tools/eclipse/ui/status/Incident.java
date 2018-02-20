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

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.annotations.SerializedName;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Captures relevant information from the <a
 * href="https://status.cloud.google.com/incidents.schema.json">Google Cloud Platform incidents
 * list</a>.
 */
public class Incident {
  enum Severity {
    OK,
    @SerializedName("low")
    LOW,
    @SerializedName("medium")
    MEDIUM,
    @SerializedName("high")
    HIGH,
    
    /** An error occurred when retrieving GCP status. */
    ERROR,
  };

  /** Return the highest severity of the given incidents. */
  @VisibleForTesting
  public static Severity getHighestSeverity(Collection<Incident> incidents) {
    Severity max = Severity.OK;
    for (Incident incident : incidents) {
      if (max.compareTo(incident.severity) < 0) {
        max = incident.severity;
      }
    }
    return max;
  }

  /** Return list of unique service names. */
  public static Collection<String> getAffectedServiceNames(Collection<Incident> incidents) {
    Set<String> serviceNames = new HashSet<>();
    for (Incident incident : incidents) {
      serviceNames.add(incident.serviceName);
    }
    return serviceNames;
  }

  @SerializedName("number")
  public int id;

  public Severity severity;
  
  @SerializedName("service_key")
  public String serviceKey;

  @SerializedName("service_name")
  public String serviceName;

  @SerializedName("external_desc")
  public String description;

  public String uri;

  public Date begin;
  public Date end;

  @Override
  public String toString() {
    return String.format("Incident %d [%s, %s]: %s", id, severity, serviceName, description);
  }
}
