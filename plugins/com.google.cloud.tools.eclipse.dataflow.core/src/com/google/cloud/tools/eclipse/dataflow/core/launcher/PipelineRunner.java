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

import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * All available {@code PipelineRunner} implementations.
 */
public enum PipelineRunner {
  DIRECT_PIPELINE_RUNNER(
      "DirectPipelineRunner",
      "com.google.cloud.dataflow.sdk.options.DirectPipelineOptions",
      "Runs the pipeline on the local machine.",
      EnumSet.of(MajorVersion.ONE)),
  DIRECT_RUNNER(
      "DirectRunner",
      "org.apache.beam.runners.direct.DirectOptions",
      "Runs the pipeline on the local machine.",
      EnumSet.of(MajorVersion.QUALIFIED_TWO, MajorVersion.TWO, MajorVersion.THREE_PLUS)),
  DATAFLOW_PIPELINE_RUNNER(
      "DataflowPipelineRunner",
      "com.google.cloud.dataflow.sdk.options.DataflowPipelineOptions",
      "Runs the pipeline remotely on the Dataflow service and detach.",
      EnumSet.of(MajorVersion.ONE)),
  DATAFLOW_RUNNER(
      "DataflowRunner",
      "org.apache.beam.runners.dataflow.options.DataflowPipelineOptions",
      "Runs the pipeline remotely on the Dataflow service and detach.",
      EnumSet.of(MajorVersion.QUALIFIED_TWO, MajorVersion.TWO, MajorVersion.THREE_PLUS)),
  BLOCKING_DATAFLOW_PIPELINE_RUNNER(
      "BlockingDataflowPipelineRunner",
      "com.google.cloud.dataflow.sdk.options.BlockingDataflowPipelineOptions",
      "Runs the pipeline remotely on the Dataflow service and wait for it to complete.",
      EnumSet.of(MajorVersion.ONE));

  private static final Map<String, PipelineRunner> RUNNERS_BY_NAME;
  private static final SetMultimap<MajorVersion, PipelineRunner> RUNNERS_IN_VERSION;

  static {
    ImmutableMap.Builder<String, PipelineRunner> runnersByName = ImmutableMap.builder();
    ImmutableSetMultimap.Builder<MajorVersion, PipelineRunner> runnersInVersion =
        ImmutableSetMultimap.builder();
    for (PipelineRunner runner : PipelineRunner.values()) {
      runnersByName.put(runner.getRunnerName(), runner);
      for (MajorVersion supported : runner.supportedVersions) {
        runnersInVersion.put(supported, runner);
      }
    }
    RUNNERS_BY_NAME = runnersByName.build();
    RUNNERS_IN_VERSION = runnersInVersion.build();
  }

  public static PipelineRunner fromRunnerName(String runnerName) {
    return RUNNERS_BY_NAME.get(runnerName);
  }

  /**
   * Return the set of all Pipeline Runners in the provided {@link MajorVersion}. Should be used
   * instead of {@link #values()}.
   */
  public static Set<PipelineRunner> inMajorVersion(MajorVersion majorVersion) {
    return RUNNERS_IN_VERSION.get(majorVersion);
  }

  private final String runnerName;
  private final String optionsName;
  private final String description;
  private final Set<MajorVersion> supportedVersions;

  PipelineRunner(
      String runnerName, String optionsName, String description, Set<MajorVersion> versions) {
    this.runnerName = runnerName;
    this.optionsName = optionsName;
    this.description = description;
    this.supportedVersions = versions;
  }

  public String getRunnerName() {
    return runnerName;
  }

  public String getOptionsClass() {
    return optionsName;
  }

  public String getDescription() {
    return description;
  }

  public Set<MajorVersion> getSupportedVersions() {
    return supportedVersions;
  }
}

