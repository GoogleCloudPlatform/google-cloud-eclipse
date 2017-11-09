/*
 * Copyright 2015 Google Inc.
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

package com.google.cloud.tools.eclipse.dataflow.core.launcher.options;

import java.util.Map;
import java.util.Set;

/**
 * An interface representing a complete PipelineOptionsHierarchy that supports querying and
 * retrieval.
 */
public interface PipelineOptionsHierarchy {
  /**
   * Retrieves a {@code PipelineOptionsType} with the given name, or null if no such type exists.
   */
  PipelineOptionsType getPipelineOptionsType(String typeName);

  /**
   * Retrieves a map of all {@link PipelineOptionsType PipelineOptionsTypes} by name.
   */
  Map<String, PipelineOptionsType> getAllPipelineOptionsTypes();

  /**
   * Returns a map from {@code PipelineOptionsType} to {@link PipelineOptionsProperty
   * PipelineOptionsProperties} defined within that {@code PipelineOptionsType}.
   */
  Map<PipelineOptionsType, Set<PipelineOptionsProperty>> getOptionsHierarchy(String... types);

  /**
   * Gets all options where at least one option is required for all {@code PipelineOptionsTypes} in
   * the supertype hierarchy of the provided {@code PipelineOptionsType} names.
   */
  Map<PipelineOptionsType, Set<PipelineOptionsProperty>> getRequiredOptionsByType(
      String... baseTypeNames);

  /**
   * Gets all properties in the {@code PipelineOptionsTypes} with the provided names and all their
   * supertypes.
   */
  Set<String> getPropertyNames(String... baseTypeNames);
}
