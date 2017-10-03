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

package com.google.cloud.tools.eclipse.appengine.libraries.ui;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;

public class AppEngineLibrariesPageTest {

  private AppEngineLibrariesPage page = new AppEngineLibrariesPage();
  
  @Rule
  public ShellTestResource shellTestResource = new ShellTestResource();
  
  @Rule
  public TestProjectCreator projectCreator = new TestProjectCreator().withFacetVersions(
      JavaFacet.VERSION_1_7);

  @Test
  public void testConstructor() {
    Assert.assertEquals("App Engine Standard Environment Libraries", page.getTitle());
    Assert.assertNull(page.getMessage());
    Assert.assertNull(page.getErrorMessage());
    Assert.assertEquals(
        "Additional jars commonly used in App Engine standard environment applications",
        page.getDescription());
    Assert.assertNotNull(page.getImage());
  }

  @Test
  public void testFinish() {
    Assert.assertTrue(page.finish());
  }

  @Test
  public void testGetSelection() {
    Assert.assertNull(page.getSelection());
  }

  @Test
  public void testGetNewContainers() {
    Shell parent = shellTestResource.getShell();
    page.initialize(projectCreator.getJavaProject(), null);
    page.createControl(parent);
    IClasspathEntry[] newContainers = page.getNewContainers();
    Assert.assertNull(newContainers); // since we haven't selected any libraries
  }

}
