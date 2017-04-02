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

package com.google.cloud.tools.eclipse.appengine.libraries.model;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class CloudLibrariesTest {

  @Test
  public void testGetLibraries() {
    List<Library> libraries = CloudLibraries.getLibraries("appengine");
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
    List<Library> libraries = CloudLibraries.getLibraries(null);
    Assert.assertTrue(libraries.isEmpty());
  }

  @Test
  public void testGetLibrary() {
    Library library = CloudLibraries.getLibrary("objectify");
    Assert.assertEquals(library.getGroup(), "appengine");
    Assert.assertEquals(library.getName(), "Objectify");
  }
  
  @Test
  public void testTransitiveDependencies() {
    Library library = CloudLibraries.getLibrary("googlecloudstorage");
    List<String> dependencies = library.getLibraryDependencies();
    Assert.assertEquals(2, dependencies.size());
    Assert.assertEquals("googlecloudcore", dependencies.get(0));
    Assert.assertEquals("googleapiclient", dependencies.get(1));
  }

}
