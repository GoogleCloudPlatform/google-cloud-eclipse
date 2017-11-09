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

package com.google.cloud.tools.eclipse.dataflow.core.launcher.options;

import static org.junit.Assert.assertEquals;

import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link PipelineOptionsNamespaces}.
 */
@RunWith(JUnit4.class)
public class PipelineOptionsNamespacesTest {
  @Test
  public void testVersionOneDataflowNamespace() {
    assertEquals(
        "com.google.cloud.dataflow.sdk.options.PipelineOptions",
        PipelineOptionsNamespaces.rootType(MajorVersion.ONE));
    assertEquals(
        "com.google.cloud.dataflow.sdk.options.Validation.Required",
        PipelineOptionsNamespaces.validationRequired(MajorVersion.ONE));
    assertEquals(
        "com.google.cloud.dataflow.sdk.options.Description",
        PipelineOptionsNamespaces.descriptionAnnotation(MajorVersion.ONE));
    assertEquals(
        "com.google.cloud.dataflow.sdk.options.Default",
        PipelineOptionsNamespaces.defaultProvider(MajorVersion.ONE));

    assertEquals(
        "groups", PipelineOptionsNamespaces.validationRequiredGroupField(MajorVersion.ONE));
  }

  @Test
  public void testVersionTwoBeamNamespace() {
    assertEquals(
        "org.apache.beam.sdk.options.PipelineOptions",
        PipelineOptionsNamespaces.rootType(MajorVersion.TWO));
    assertEquals(
        "org.apache.beam.sdk.options.Validation.Required",
        PipelineOptionsNamespaces.validationRequired(MajorVersion.TWO));
    assertEquals(
        "org.apache.beam.sdk.options.Description",
        PipelineOptionsNamespaces.descriptionAnnotation(MajorVersion.TWO));
    assertEquals(
        "org.apache.beam.sdk.options.Default",
        PipelineOptionsNamespaces.defaultProvider(MajorVersion.TWO));

    assertEquals(
        "groups", PipelineOptionsNamespaces.validationRequiredGroupField(MajorVersion.TWO));
  }
}
