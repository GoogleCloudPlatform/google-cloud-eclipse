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

package com.google.cloud.tools.eclipse.appengine.newproject.flex;

import org.junit.Assert;
import org.junit.Test;

public class AppEngineFlexProjectWizardTest {

  private AppEngineFlexProjectWizard wizard = new AppEngineFlexProjectWizard();

  @Test
  public void testWindowTitle() {
    Assert.assertEquals("New App Engine Flexible Project", wizard.getWindowTitle());
  }

  @Test
  public void testValidateDependencies() {
    Assert.assertTrue(wizard.validateDependencies(true, true).isOK());
  }
  
  @Test
  public void testAddPages() {
    wizard.addPages();
    Assert.assertFalse(wizard.canFinish());
    Assert.assertEquals(1, wizard.getPageCount());
    Assert.assertNotNull(wizard.getPage("basicNewProjectPage"));
  }
  
}
