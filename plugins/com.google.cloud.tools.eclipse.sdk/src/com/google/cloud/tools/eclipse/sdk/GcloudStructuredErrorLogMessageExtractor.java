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
import com.google.common.base.Function;
import java.util.Locale;

/**
 * A {@link ProcessOutputLineListener} that extracts error messages from gcloud structured log
 * output to stderr. If an output line is a structured JSON log whose {@code verbosity} property
 * is {@code "ERROR"}, returns its {@code message} property.
 */
public class GcloudStructuredErrorLogMessageExtractor implements Function<String, String> {

  @Override
  public String apply(String line) {
    try {
      GcloudStructuredLog error = GcloudStructuredLog.parse(line);
      if (error.getVerbosity().toUpperCase(Locale.US).equals("ERROR")) {
        return error.getMessage();
      }
    } catch (JsonParseException e) {  // syntax or semantic parsing error: fall through
    }
    return null;
  }
}
