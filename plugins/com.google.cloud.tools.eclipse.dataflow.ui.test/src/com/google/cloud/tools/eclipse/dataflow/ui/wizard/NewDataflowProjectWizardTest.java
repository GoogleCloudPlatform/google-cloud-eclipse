/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.dataflow.ui.wizard;

import org.junit.Assert;
import org.junit.Test;

public class NewDataflowProjectWizardTest {

  private NewDataflowProjectWizard wizard = new NewDataflowProjectWizard();

  @Test
  public void testInit() {
    wizard.init(null, null);
    Assert.assertEquals("New Cloud Dataflow Project", wizard.getWindowTitle());
    Assert.assertTrue(wizard.isHelpAvailable());
    Assert.assertTrue(wizard.needsProgressMonitor());
  }

  @Test
  public void testAddPages() {
    wizard.addPages();
    Assert.assertFalse(wizard.canFinish());
    Assert.assertEquals(2, wizard.getPageCount());
    Assert.assertNotNull(wizard.getPage("newDataflowProjectWizardLandingPage"));
    Assert.assertNotNull(wizard.getPage("dataflowDefaultRunOptionsPage"));
  }

}
