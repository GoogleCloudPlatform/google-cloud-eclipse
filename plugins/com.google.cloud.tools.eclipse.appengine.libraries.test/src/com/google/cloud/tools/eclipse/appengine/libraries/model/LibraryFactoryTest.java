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

package com.google.cloud.tools.eclipse.appengine.libraries.model;

import org.eclipse.core.runtime.IConfigurationElement;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

public class LibraryFactoryTest {
  
  private LibraryFactory factory = new LibraryFactory();
  
  @Test
  public void testCreate() throws LibraryFactoryException {
    IConfigurationElement configuration = Mockito.mock(IConfigurationElement.class);
    Mockito.when(configuration.getAttribute("id")).thenReturn("guava");
    Mockito.when(configuration.getAttribute("group")).thenReturn("com.google.guava");
    Mockito.when(configuration.getName()).thenReturn("library");
    Mockito.when(configuration.getAttribute("siteUri"))
        .thenReturn(
        "https://cloud.google.com/storage/docs/reference/libraries#client-libraries-install-java");
    Mockito.when(configuration.getChildren("libraryFile"))
        .thenReturn(new IConfigurationElement[0]);
    Mockito.when(configuration.getChildren("libraryDependency"))
        .thenReturn(new IConfigurationElement[0]);

    Library library = factory.create(configuration);
    Assert.assertEquals("com.google.guava", library.getGroup());
  }
  
  @Test
  public void testCreate_null() throws LibraryFactoryException {
    try {
      factory.create(null);
      Assert.fail();
    } catch (NullPointerException ex) {
    } 
  }
  
  @Test
  public void testCreate_nonLibrary() {
    IConfigurationElement configuration = Mockito.mock(IConfigurationElement.class);
    try {
      factory.create(configuration);
      Assert.fail();
    } catch (LibraryFactoryException ex) {
      Assert.assertNotNull(ex.getMessage());
    } 
  }
  
}
