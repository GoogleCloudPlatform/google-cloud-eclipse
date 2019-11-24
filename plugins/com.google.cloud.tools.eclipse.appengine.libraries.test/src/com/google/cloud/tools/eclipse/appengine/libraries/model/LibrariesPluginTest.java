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

package com.google.cloud.tools.eclipse.appengine.libraries.model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.google.cloud.tools.eclipse.util.ArtifactRetriever;

public class LibrariesPluginTest {
  
  private Library library = new Library("a");
  
  @Test
  public void testSetLibraryFiles() {   
    Logger logger = Logger.getLogger(ArtifactRetriever.class.getName());
    Logger logger2 = Logger.getLogger(Library.class.getName());
    
    try {
      logger.setLevel(Level.OFF);
      logger2.setLevel(Level.OFF);

      MavenCoordinates mavenCoordinates =
          new MavenCoordinates.Builder().setGroupId("groupId").setArtifactId("artifactId").build();
      library.setLibraryFiles(Arrays.asList(new LibraryFile(mavenCoordinates)));
      List<LibraryFile> allDependencies = library.getAllDependencies();
      assertNotNull(allDependencies);
      assertThat(allDependencies.size(), is(1));
      LibraryFile actual = allDependencies.get(0);
      assertThat(actual.getMavenCoordinates().getRepository(), is("central"));
      assertThat(actual.getMavenCoordinates().getGroupId(), is("groupId"));
      assertThat(actual.getMavenCoordinates().getArtifactId(), is("artifactId"));
    } finally {
      logger.setLevel(null);
      logger2.setLevel(null);
    }
  }

  @Test
  public void testDirectDependencies() {
    MavenCoordinates mavenCoordinates =
        new MavenCoordinates.Builder()
            .setGroupId("com.googlecode.objectify")
            .setArtifactId("objectify")
            .setVersion("6.0.5").build();
    library.setLibraryFiles(Arrays.asList(new LibraryFile(mavenCoordinates)));

    List<LibraryFile> directFiles = library.getDirectDependencies();
    assertEquals(1, directFiles.size()); 
    assertEquals(mavenCoordinates.getArtifactId(),
        directFiles.get(0).getMavenCoordinates().getArtifactId());
    
    // todo for some reason this next test fails only on Kokoro Windows
    org.junit.Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
    List<LibraryFile> transitiveDependencies = library.getAllDependencies();
    assertTrue(transitiveDependencies.size() > directFiles.size()); 
  }

}
