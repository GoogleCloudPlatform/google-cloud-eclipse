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

import com.google.cloud.tools.eclipse.ui.status.Incident.Severity;
import java.util.Collection;

/** Summary of current Google Cloud Platform status based on accumulated incidents. */
public class GcpStatus {
  public static final GcpStatus OK_STATUS =
      new GcpStatus(Severity.OK, "All services available", null);

  public Severity severity;
  public String summary;
  public Collection<Incident> active;

  public GcpStatus(Severity severity, String summary, Collection<Incident> active) {
    this.severity = severity;
    this.summary = summary;
    this.active = active;
  }

  public String toString() {
    return severity + ": " + summary;
  }
}
