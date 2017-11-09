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

public class DataflowProjectValidationStatusTest {

  @Test
  public void testOk() {
    Assert.assertTrue(DataflowProjectValidationStatus.OK.isValid());
    Assert.assertFalse(DataflowProjectValidationStatus.OK.isError());
    Assert.assertFalse(DataflowProjectValidationStatus.OK.isMissing());
  }
  
  @Test
  public void testNotOk() {
    Assert.assertFalse(DataflowProjectValidationStatus.ILLEGAL_ARTIFACT_ID.isMissing());
    Assert.assertFalse(DataflowProjectValidationStatus.ILLEGAL_ARTIFACT_ID.isValid());
    Assert.assertTrue(DataflowProjectValidationStatus.ILLEGAL_ARTIFACT_ID.isError());
  }
  
  @Test
  public void testNotEntered() {
    Assert.assertFalse(DataflowProjectValidationStatus.NO_ARTIFACT_ID.isValid());
    Assert.assertFalse(DataflowProjectValidationStatus.NO_ARTIFACT_ID.isError());
    Assert.assertTrue(DataflowProjectValidationStatus.NO_ARTIFACT_ID.isMissing());
  }

}
