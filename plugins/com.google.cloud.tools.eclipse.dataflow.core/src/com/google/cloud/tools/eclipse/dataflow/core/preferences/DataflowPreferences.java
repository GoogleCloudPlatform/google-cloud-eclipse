/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.preferences;

import com.google.common.collect.ImmutableSet;
import java.util.Map;

/**
 * Retrieves preferences stored for the Dataflow plugin.
 */
public interface DataflowPreferences {
  /**
   * The PipelineOptions property representing the GCP project to run in.
   */
  String PROJECT_PROPERTY = "project";

  /**
   * The PipelineOptions property representing the staging location for dataflow-related artifacts.
   */
  String STAGING_LOCATION_PROPERTY = "stagingLocation";

  /**
   * The PipelineOptions property representing the temporary location to use on GCP.
   */
  String GCP_TEMP_LOCATION_PROPERTY = "gcpTempLocation";

  /**
   * All properties that can be used for defaulting options in a Pipeline Launch.
   */
  ImmutableSet<String> SUPPORTED_DEFAULT_PROPERTIES =
      ImmutableSet.of(PROJECT_PROPERTY, STAGING_LOCATION_PROPERTY, GCP_TEMP_LOCATION_PROPERTY);

  /**
   * Gets the default project.
   */
  String getDefaultProject();

  /**
   * Gets the default staging location.
   */
  String getDefaultStagingLocation();

  /**
   * Gets the default GCP Temp location.
   */
  String getDefaultGcpTempLocation();

  /**
   * Gets the preferences as a Map from property name to value, removing any absent properties.
   */
  Map<String, String> asDefaultPropertyMap();
}

