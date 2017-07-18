/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.project;

import com.google.common.collect.ImmutableSortedSet;
import java.util.NavigableSet;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class DataflowMavenCoordinates {

  static final String DATAFLOW_GROUP_ID = "com.google.cloud.dataflow";
  static final String DATAFLOW_SDK_ARTIFACT = "google-cloud-dataflow-java-sdk-all";

  /** Versions which are known to have been released. */
  static final NavigableSet<ArtifactVersion> KNOWN_VERSIONS =
      ImmutableSortedSet.<ArtifactVersion>of(
          new DefaultArtifactVersion("1.9.0"), new DefaultArtifactVersion("2.0.0"));
}
