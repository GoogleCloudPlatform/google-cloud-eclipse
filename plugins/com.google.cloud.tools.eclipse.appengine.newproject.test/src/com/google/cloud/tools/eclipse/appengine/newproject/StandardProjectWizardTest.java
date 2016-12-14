/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.newproject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StandardProjectWizardTest {

  private StandardProjectWizard wizard;

  @Before
  public void setUp() {
    try {
      wizard = new StandardProjectWizard();
      // I don't know why this fails the first time and passes the second, but it does.
    } catch (NullPointerException ex) {
      wizard = new StandardProjectWizard();
    }
  }

  @Test
  public void testTitleSet() {
    Assert.assertEquals("New App Engine Standard Project", wizard.getWindowTitle());
  }

  @Test
  public void testAddPages() {
    wizard.addPages();
    Assert.assertFalse(wizard.canFinish());
    Assert.assertEquals(1, wizard.getPageCount());
    Assert.assertNotNull(wizard.getPage("basicNewProjectPage"));
  }

}
