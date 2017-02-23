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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assert;
import org.junit.Test;

public class AppEngineStandardWizardPageTest {

  private AppEngineStandardWizardPage page = new AppEngineStandardWizardPage();

  @Test
  public void testPageInitiallyIncomplete() {
    Assert.assertFalse(page.isPageComplete());
  }
  
  @Test
  public void testGetNextPage() {
    Assert.assertNull(page.getNextPage());
  }
  
  @Test
  public void testGetPreviousPage() {
    Assert.assertNull(page.getPreviousPage());
  }
  
  @Test
  public void testTitle() {
    Assert.assertEquals("App Engine Standard Project", page.getTitle());
  }
  
  @Test
  public void testDescription() {
    Assert.assertEquals(
        "Create a new Eclipse project for App Engine standard environment development.",
        page.getDescription());
  }
  
  @Test
  public void testValidatePage() {
    Assert.assertFalse(page.validatePage());
    Assert.assertNull(page.getErrorMessage());
    Assert.assertEquals("Project name must be specified", page.getMessage());
  }
  
  @Test
  public void testCreateControl() {
    Composite parent = new Composite(new Shell(), SWT.NONE);
    page.createControl(parent);
    Assert.assertNull(page.getErrorMessage());
    Assert.assertEquals("Enter a project name.", page.getMessage());
  }
}
