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

import org.junit.Assert;
import org.junit.Test;

import com.google.cloud.tools.eclipse.util.MavenCoordinatesValidator;

public class DataflowProjectCreatorTest {

  @Test
  public void testCreate() {
    Assert.assertNotNull(DataflowProjectCreator.create());
  }
  
  @Test
  public void testTemplates() {
    for (DataflowProjectCreator.Template template : DataflowProjectCreator.Template.values()) {
      Assert.assertTrue(MavenCoordinatesValidator.validateArtifactId(template.getArchetype()));
      Assert.assertFalse(template.getSdkVersions().isEmpty());
      Assert.assertFalse(template.getLabel().isEmpty());
    }
  }

}
