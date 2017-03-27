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

package com.google.cloud.tools.eclipse.dataflow.core.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import java.util.Arrays;

/**
 * Tests for {@link PipelineRunner}.
 */
@RunWith(Parameterized.class)
public class PipelineRunnerTest {
  private final PipelineRunner runner;

  @Parameters
  public static Iterable<?> params() {
    return Arrays.asList(PipelineRunner.values());
  }

  public PipelineRunnerTest(PipelineRunner runner) {
    this.runner = runner;
  }

  @Test
  public void testToFromName() {
    assertEquals(runner, PipelineRunner.fromRunnerName(runner.getRunnerName()));
  }

  @Test
  public void testVersions() {
    assertFalse(
        "Runner " + runner + " has no supported versions", runner.getSupportedVersions().isEmpty());
    for (MajorVersion majorVersion : runner.getSupportedVersions()) {
      assertTrue(
          "Runner " + runner + " not contained in result of inMajorVersion " + majorVersion,
          PipelineRunner.inMajorVersion(majorVersion).contains(runner));
    }
  }
}
