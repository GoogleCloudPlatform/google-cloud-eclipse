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
      Assert.assertFalse(library.getAllDependencies().isEmpty());
      String tooltip = library.getToolTip();
      Assert.assertFalse(tooltip.isEmpty());
      Assert.assertFalse(tooltip, tooltip.startsWith("!"));
    }
    Assert.assertEquals(3, libraries.size());
  }

  @Test
  public void testGetClientApis() {
    List<Library> libraries = CloudLibraries.getLibraries("clientapis");
    Assert.assertTrue(libraries.size() > 10);
    
    for (Library library : libraries) {
      String tooltip = library.getToolTip();
      
      Assert.assertNotNull(library.getName() + " has no tooltip", tooltip);
      Assert.assertFalse(tooltip.isEmpty());
      Assert.assertFalse(library.getName() + " has no files", library.getAllDependencies().isEmpty());
      Assert.assertEquals("clientapis", library.getGroups().get(0));
    }
  }
  
  @Test
  public void testGrpcTransport() {
    Library library = CloudLibraries.getLibrary("datalossprevention");
    Assert.assertEquals("grpc", library.getTransport());
  }
  
  @Test
  public void testHttpTransport() {
    Library library = CloudLibraries.getLibrary("googlecloudstorage");
    Assert.assertEquals("http", library.getTransport());
  }
  
  @Test
  public void testGetLibraries_null() {
    List<Library> libraries = CloudLibraries.getLibraries(null);
    Assert.assertTrue(libraries.isEmpty());
  }

  @Test
  public void testGetLibrary() {
    Library library = CloudLibraries.getLibrary("objectify");
    Assert.assertEquals("appengine", library.getGroups().get(0));
    Assert.assertEquals("Objectify", library.getName());
  }
  
  @Test
  public void testGetLibraryObjectify6() {
    Library library = CloudLibraries.getLibrary("objectify6");
    Assert.assertEquals("flexible", library.getGroups().get(0));
    Assert.assertEquals("Objectify", library.getName());
  }
}
