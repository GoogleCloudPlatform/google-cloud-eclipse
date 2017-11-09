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

package com.google.cloud.tools.eclipse.dataflow.core.launcher;

import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsHierarchy;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsProperty;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsType;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A {@link PipelineOptionsHierarchy} that returns exclusively empty or false values.
 */
public class EmptyPipelineOptionsHierarchy implements PipelineOptionsHierarchy {
  @Override
  public PipelineOptionsType getPipelineOptionsType(String typeName) {
    return null;
  }

  @Override
  public Map<String, PipelineOptionsType> getAllPipelineOptionsTypes() {
    return Collections.emptyMap();
  }

  @Override
  public Map<PipelineOptionsType, Set<PipelineOptionsProperty>> getOptionsHierarchy(
      String... types) {
    return Collections.emptyMap();
  }

  @Override
  public Map<PipelineOptionsType, Set<PipelineOptionsProperty>> getRequiredOptionsByType(
      String... baseTypeNames) {
    return Collections.emptyMap();
  }

  @Override
  public Set<String> getPropertyNames(String... baseTypeNames) {
    return Collections.emptySet();
  }
}
