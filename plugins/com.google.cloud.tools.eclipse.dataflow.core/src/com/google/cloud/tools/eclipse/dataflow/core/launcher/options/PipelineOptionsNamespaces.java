/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.google.cloud.tools.eclipse.dataflow.core.launcher.options;

import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * A collection of utility methods for constructing version-based constants that relate to
 * {@code PipelineOptions}.
 */
public final class PipelineOptionsNamespaces {
  private PipelineOptionsNamespaces() {}

  private static final String DATAFLOW_OPTIONS_BASE = "com.google.cloud.dataflow.sdk.options";
  private static final String BEAM_OPTIONS_BASE = "org.apache.beam.sdk.options";

  private static final Map<MajorVersion, String> VERSION_PREFIXES =
      ImmutableMap.<MajorVersion, String>builder()
          .put(MajorVersion.ONE, DATAFLOW_OPTIONS_BASE + ".")
          .put(MajorVersion.QUALIFIED_TWO, BEAM_OPTIONS_BASE + ".")
          .put(MajorVersion.TWO, BEAM_OPTIONS_BASE + ".")
          .put(MajorVersion.THREE_PLUS, BEAM_OPTIONS_BASE + ".")
          .build();
  private static final String VALIDATION_ANNOTATION = "Validation.Required";
  private static final String VALIDATION_REQUIRED_GROUPS = "groups";

  private static final String DEFAULT_ANNOTATION = "Default";
  private static final String DESCRIPTION_ANNOTATION = "Description";

  private static final String ROOT_OPTIONS_TYPE = "PipelineOptions";

  public static String validationRequired(MajorVersion majorVersion) {
    return VERSION_PREFIXES.get(majorVersion) + VALIDATION_ANNOTATION;
  }

  public static String validationRequiredGroupField(
      @SuppressWarnings("unused") MajorVersion majorVersion) {
    return VALIDATION_REQUIRED_GROUPS;
  }

  public static String defaultProvider(MajorVersion majorVersion) {
    return VERSION_PREFIXES.get(majorVersion) + DEFAULT_ANNOTATION;
  }

  public static String descriptionAnnotation(MajorVersion majorVersion) {
    return VERSION_PREFIXES.get(majorVersion) + DESCRIPTION_ANNOTATION;
  }

  public static String rootType(MajorVersion version) {
    return VERSION_PREFIXES.get(version) + ROOT_OPTIONS_TYPE;
  }
}
