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

/**
 * Configuration attributes used in the {@link DataflowPipelineLaunchDelegate}.
 */
public enum PipelineConfigurationAttr {
  /**
   * All of the arguments provided to the Launch Configuration over the life of the configuration.
   */
  ALL_ARGUMENT_VALUES("ALL_ARGUMENT_VALUES"),
  /** The runner to use in this launch. */
  RUNNER_ARGUMENT("RUNNER_ARGUMENT"),
  /** The class name for a custom user options class. */
  USER_OPTIONS_NAME("USER_OPTIONS_CLASS_NAME"),
  /** If launch options are defaulted when possible or are custom. */
  USE_DEFAULT_LAUNCH_OPTIONS("USE_DEFAULT_LAUNCH_OPTIONS");

  private static final String DATAFLOW_NS = "com.google.cloud.dataflow.eclipse.";

  private final String attributeName;

  private PipelineConfigurationAttr(String attributeName) {
    this.attributeName = DATAFLOW_NS + attributeName;
  }

  @Override
  public String toString() {
    return attributeName;
  }
}
