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

/**
 * Enumeration of the Archetype templates available for project creation.
 */
public enum DataflowProjectArchetype {
  STARTER_POM_WITH_PIPELINE(
      "Starter project with a simple pipeline",
      "google-cloud-dataflow-java-archetypes-starter",
      ImmutableSortedSet.of(MajorVersion.QUALIFIED_TWO, MajorVersion.TWO)),
  EXAMPLES(
      "Example pipelines",
      "google-cloud-dataflow-java-archetypes-examples",
      ImmutableSortedSet.of(MajorVersion.QUALIFIED_TWO, MajorVersion.TWO));

  private final String label;
  private final String artifactId;
  private final ImmutableSortedSet<MajorVersion> sdkVersions;

  private DataflowProjectArchetype(String label, String artifactId,
      NavigableSet<MajorVersion> sdkVersions) {
    this.label = label;
    this.artifactId = artifactId;
    this.sdkVersions = ImmutableSortedSet.copyOf(sdkVersions);
  }

  public String getLabel() {
    return label;
  }

  /**
   * @return the artifact ID of the archetype
   */
  public String getArtifactId() {
    return artifactId;
  }

  public NavigableSet<MajorVersion> getSdkVersions() {
    return sdkVersions;
  }
}