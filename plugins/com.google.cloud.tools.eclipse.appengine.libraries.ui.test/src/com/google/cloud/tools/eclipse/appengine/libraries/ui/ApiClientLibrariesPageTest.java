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

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ApiClientLibrariesPageTest {

  private final ApiClientLibrariesPage page = new ApiClientLibrariesPage();

  @Test
  public void testConstructor() {
    Assert.assertEquals("Google Client APIs for Java", page.getTitle());
    Assert.assertNull(page.getMessage());
    Assert.assertNull(page.getErrorMessage());
    Assert.assertEquals(
        "Additional jars used by Google Client APIs for Java",
        page.getDescription());
    Assert.assertNotNull(page.getImage());
  }

  @Test
  public void testSetSelection_null() {
    page.setSelection(null);
  }
  
  @Test
  public void testGetSelection() {
    Assert.assertNull(page.getSelection());
  }

  @Test
  public void testSelectionRoundTrip() {
    List<Library> selectedLibraries = Arrays.asList(new Library("foo"));
    page.setSelectedLibraries(selectedLibraries);

    List<Library> returnedLibraries = page.getSelectedLibraries();
    Assert.assertEquals(1, returnedLibraries.size());
    Assert.assertEquals("foo", returnedLibraries.get(0).getId());
  }
}
