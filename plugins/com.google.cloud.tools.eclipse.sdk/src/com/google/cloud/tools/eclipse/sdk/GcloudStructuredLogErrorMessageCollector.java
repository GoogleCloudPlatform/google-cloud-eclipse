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

package com.google.cloud.tools.eclipse.sdk;

import com.google.cloud.tools.appengine.cloudsdk.JsonParseException;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.appengine.cloudsdk.serialization.GcloudStructuredLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A {@link ProcessOutputLineListener} that extracts error messages from gcloud structured logs.
 * Should listen to stderr. If an output line is a structured JSON log whose {@code verbosity}
 * property is {@code "ERROR"}, its {@code message} property is extracted and collected.
 */
public class GcloudStructuredLogErrorMessageCollector implements ProcessOutputLineListener {

  private final List<String> errorMessages = new ArrayList<>();

  @Override
  public void onOutputLine(String line) {
    try {
      GcloudStructuredLog log = GcloudStructuredLog.parse(line);
      if (log != null) {
        String verbosity = log.getVerbosity();
        if (verbosity != null && verbosity.toUpperCase(Locale.US).equals("ERROR")) {
          if (log.getMessage() == null || log.getMessage().trim().isEmpty()) {
            errorMessages.add("no error message provided");
          } else {
            errorMessages.add(log.getMessage());
          }
        }
      }
    } catch (JsonParseException e) {
      // syntax or semantic parsing error; not a structured error log line
    }
  }

  public List<String> getErrorMessages() {
    return new ArrayList<>(errorMessages);
  }
}
