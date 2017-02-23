/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.libraries;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;

public class AppEngineLibrariesTest {
  
  @Test
  public void testGetLibraries() {
    List<Library> libraries = AppEngineLibraries.getLibraries("appengine");
    for (Library library : libraries) {
      Assert.assertFalse(library.getLibraryFiles().isEmpty());
      String tooltip = library.getToolTip();
      Assert.assertFalse(tooltip.isEmpty());
      Assert.assertFalse(tooltip, tooltip.startsWith("!"));
    }
    Assert.assertEquals(3, libraries.size());
  }
  
  @Test
  public void testGetLibraries_null() {
    List<Library> libraries = AppEngineLibraries.getLibraries(null);
    Assert.assertTrue(libraries.isEmpty());
  }
  
  @Test
  public void testGetLibrary() {
    Library library = AppEngineLibraries.getLibrary("objectify");
    Assert.assertEquals(library.getGroup(), "appengine");
    Assert.assertEquals(library.getName(), "Objectify");
  }

}
