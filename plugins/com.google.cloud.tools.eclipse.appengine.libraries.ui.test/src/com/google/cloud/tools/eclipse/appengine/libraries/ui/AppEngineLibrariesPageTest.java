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

import org.junit.Assert;
import org.junit.Test;

public class AppEngineLibrariesPageTest {

  private AppEngineLibrariesPage page = new AppEngineLibrariesPage();

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

}
