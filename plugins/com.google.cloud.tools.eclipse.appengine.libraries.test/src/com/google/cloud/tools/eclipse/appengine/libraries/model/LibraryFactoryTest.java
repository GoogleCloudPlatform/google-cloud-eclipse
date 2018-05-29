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

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.eclipse.core.runtime.IConfigurationElement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class LibraryFactoryTest {

  // todo is there a simple way to load this up from XML instead?
  private IConfigurationElement configuration;
  private IConfigurationElement[] libraryFiles = new IConfigurationElement[1];
  private IConfigurationElement[] mavenCoordinates = new IConfigurationElement[1];

  @Before
  public void setUp() {
    configuration = Mockito.mock(IConfigurationElement.class);
    Mockito.when(configuration.getAttribute("id")).thenReturn("guava");

    libraryFiles[0] = Mockito.mock(IConfigurationElement.class);
    Mockito.when(libraryFiles[0].getAttribute("mavenCoordinates")).thenReturn("mavenCoordinates");
    Mockito.when(libraryFiles[0].getChildren()).thenReturn(new IConfigurationElement[0]);
    mavenCoordinates[0] = Mockito.mock(IConfigurationElement.class);
    Mockito.when(mavenCoordinates[0].getAttribute("groupId")).thenReturn("com.google.guava");
    Mockito.when(mavenCoordinates[0].getAttribute("artifactId")).thenReturn("guava");
    
    Mockito.when(configuration.getAttribute("group")).thenReturn("com.google.guava");
    Mockito.when(configuration.getAttribute("javaVersion")).thenReturn("1.8");
    Mockito.when(configuration.getName()).thenReturn("library");
    Mockito.when(configuration.getAttribute("siteUri"))
        .thenReturn(
        "https://cloud.google.com/storage/docs/reference/libraries#client-libraries-install-java");
    Mockito.when(configuration.getChildren("libraryDependency"))
        .thenReturn(new IConfigurationElement[0]);
    
    Mockito.when(libraryFiles[0].getChildren("mavenCoordinates")).thenReturn(mavenCoordinates);
    Mockito.when(libraryFiles[0].getName()).thenReturn("libraryFile");
  }
  
  @Test
  public void testCreate_useLatestVersion() throws LibraryFactoryException {
    Mockito.when(configuration.getChildren("libraryFile")).thenReturn(libraryFiles);
    
    Library library = LibraryFactory.create(configuration);
    String version = library.getAllDependencies().get(0).getMavenCoordinates().getVersion();
    DefaultArtifactVersion artifactVersion = new DefaultArtifactVersion(version);
    int majorVersion = artifactVersion.getMajorVersion();
    Assert.assertTrue(majorVersion >= 22);
  }
  
  @Test
  public void testCreate_useSpecificVersion() throws LibraryFactoryException {
    Mockito.when(configuration.getChildren("libraryFile")).thenReturn(libraryFiles);
    Mockito.when(mavenCoordinates[0].getAttribute("version")).thenReturn("19.0");
    Mockito.when(libraryFiles[0].getAttribute("pinned")).thenReturn("true");

    Library library = LibraryFactory.create(configuration);
    String version = library.getAllDependencies().get(0).getMavenCoordinates().getVersion();
    int majorVersion = new DefaultArtifactVersion(version).getMajorVersion();
    Assert.assertEquals(19, majorVersion);
  }

  @Test
  public void testCreate() throws LibraryFactoryException {
    Mockito.when(configuration.getChildren("libraryFile")).thenReturn(new IConfigurationElement[0]);

    Library library = LibraryFactory.create(configuration);
    Assert.assertEquals("com.google.guava", library.getGroups().get(0));
    Assert.assertEquals("1.8", library.getJavaVersion());
    Assert.assertTrue(library.isExport());
  }

  @Test
  public void testCreate_nonDefaults() throws LibraryFactoryException {
    Mockito.when(configuration.getChildren("libraryFile")).thenReturn(new IConfigurationElement[0]);
    Mockito.when(configuration.getAttribute("dependencies")).thenReturn("include");
    Mockito.when(configuration.getAttribute("export")).thenReturn("false");

    Library library = LibraryFactory.create(configuration);
    Assert.assertFalse(library.isExport());
  }

  @Test
  public void testCreate_null() throws LibraryFactoryException {
    try {
      LibraryFactory.create(null);
      Assert.fail();
    } catch (NullPointerException ex) {
    }
  }

  @Test
  public void testCreate_nonLibrary() {
    try {
      LibraryFactory.create(Mockito.mock(IConfigurationElement.class));
      Assert.fail();
    } catch (LibraryFactoryException ex) {
      Assert.assertNotNull(ex.getMessage());
    }
  }

}
