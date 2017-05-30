/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.page;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NewDataflowProjectWizardDefaultRunOptionsPageTest {
  
  private NewDataflowProjectWizardDefaultRunOptionsPage page;
  
  @Before
  public void setUp() {
    page = new NewDataflowProjectWizardDefaultRunOptionsPage(null);
  }
  
  @Test
  public void testPageComplete() {
    Assert.assertTrue(page.isPageComplete());
  }
  
  @Test
  public void testTitle() {
    Assert.assertEquals("Set Default Cloud Dataflow Run Options", page.getTitle());
  }

  @Test
  public void testDescription() {
    Assert.assertEquals(
        "Set default options for running a Dataflow Pipeline.", page.getDescription());
  }
}
