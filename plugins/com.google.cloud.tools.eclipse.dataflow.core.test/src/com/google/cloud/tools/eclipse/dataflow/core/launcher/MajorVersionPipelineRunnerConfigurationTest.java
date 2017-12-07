/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import java.util.Arrays;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.AdditionalAnswers;

/**
 * Test that each {@link MajorVersion} has associated configuration settings in the
 * {@code PipelineRunner#RUNNERS_IN_VERSION} mapping table and {@link PipelineLaunchConfiguration}.
 */
@RunWith(Parameterized.class)
public class MajorVersionPipelineRunnerConfigurationTest {

  @Parameters(name = "{0}")
  public static Iterable<? extends Object> majorVersions() {
    return Arrays.asList(MajorVersion.values());
  }

  private final MajorVersion majorVersion;

  public MajorVersionPipelineRunnerConfigurationTest(MajorVersion majorVersion) {
    this.majorVersion = majorVersion;
  }

  @Test
  public void testHasDefaultRunner() {
    PipelineRunner defaultRunner = PipelineLaunchConfiguration.defaultRunner(majorVersion);
    assertNotNull(defaultRunner);
  }

  @Test
  public void testHasConfiguredRunners() {
    Set<PipelineRunner> runners = PipelineRunner.inMajorVersion(majorVersion);
    assertNotEquals(0, runners.size());
  }

  @Test
  public void testDefaultRunnerInConfiguredRunners() {
    PipelineRunner defaultRunner = PipelineLaunchConfiguration.defaultRunner(majorVersion);
    assertNotNull(defaultRunner);
    Set<PipelineRunner> runners = PipelineRunner.inMajorVersion(majorVersion);
    assertThat(runners, CoreMatchers.hasItem(defaultRunner));
  }

  @Test
  public void testFromLaunchConfiguration_defaultRunner() throws CoreException {
    ILaunchConfiguration empty =
        mock(ILaunchConfiguration.class, AdditionalAnswers.returnsSecondArg());
    PipelineLaunchConfiguration launchConfiguration =
        PipelineLaunchConfiguration.fromLaunchConfiguration(majorVersion, empty);
    assertEquals(PipelineLaunchConfiguration.defaultRunner(majorVersion), launchConfiguration.getRunner());
  }

}
