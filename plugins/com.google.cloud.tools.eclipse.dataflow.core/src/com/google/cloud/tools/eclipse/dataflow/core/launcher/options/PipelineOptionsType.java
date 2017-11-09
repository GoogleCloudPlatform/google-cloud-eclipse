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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.Set;

/**
 * A type that extends the Dataflow {@code PipelineOptions} interface. Maintains a reference to all
 * of the {@code PipelineOptionsTypes} that are parents of this class.
 */
public class PipelineOptionsType {
  private final String name;
  private final Set<PipelineOptionsType> parentTypes;
  private final Set<PipelineOptionsProperty> declaredProperties;

  private final long weight;

  public PipelineOptionsType(String name, Set<PipelineOptionsType> parentTypes,
      Set<PipelineOptionsProperty> properties) {
    this.name = name;
    this.parentTypes = parentTypes;
    this.declaredProperties = properties;
    long weight = 1;
    for (PipelineOptionsType parentType : parentTypes) {
      weight += parentType.weight;
    }
    this.weight = weight;
  }

  public Set<PipelineOptionsProperty> getDeclaredProperties() {
    return declaredProperties;
  }

  public Set<PipelineOptionsType> getDirectSuperInterfaces() {
    return parentTypes;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(PipelineOptionsType.class).add("name", name).toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof PipelineOptionsType)) {
      return false;
    }
    PipelineOptionsType that = (PipelineOptionsType) obj;
    return Objects.equal(this.name, that.name);
  }

  long getWeight() {
    return weight;
  }
}
