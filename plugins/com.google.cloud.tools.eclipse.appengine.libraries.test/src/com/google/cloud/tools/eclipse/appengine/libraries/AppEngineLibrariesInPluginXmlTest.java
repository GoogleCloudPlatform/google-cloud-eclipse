/*******************************************************************************
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
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.RegistryFactory;
import org.junit.Test;

import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryBuilder;
import com.google.cloud.tools.eclipse.appengine.libraries.config.LibraryBuilder.LibraryBuilderException;

/**
 * This class is intended to test the App Engine libraries set in the plugin.xml to validate that their attributes
 * are correctly set.
 */
public class AppEngineLibrariesInPluginXmlTest {

  private static final String APP_ENGINE_API_LIBRARY_ID = "appengine-api";

  @Test
  public void testAppEngineAPIConfig() throws URISyntaxException, LibraryBuilderException {
    IConfigurationElement[] configurationElements =
        RegistryFactory.getRegistry()
          .getConfigurationElementsFor(AppEngineLibraryContainerInitializer.LIBRARIES_EXTENSION_POINT);
    for (IConfigurationElement configurationElement : configurationElements) {
      if (configurationElement.getAttribute("id").equals(APP_ENGINE_API_LIBRARY_ID)) {
        Library appEngineLibrary = new LibraryBuilder().build(configurationElement);
        verifyAppEngineLibrary(appEngineLibrary);
        return;
      }
    }
    fail("App Engine API library with ID " + APP_ENGINE_API_LIBRARY_ID + " was not found in the plugin.xml");
  }

  private void verifyAppEngineLibrary(Library appEngineLibrary) throws URISyntaxException {
    assertThat(appEngineLibrary.getContainerPath().toString(),
               is(Library.CONTAINER_PATH_PREFIX + "/" + APP_ENGINE_API_LIBRARY_ID));
    assertThat(appEngineLibrary.getId(), is(APP_ENGINE_API_LIBRARY_ID));
    assertThat(appEngineLibrary.getName(), is("App Engine API"));
    assertThat(appEngineLibrary.getSiteUri(), is(new URI("https://cloud.google.com/appengine/docs/java/")));

    assertThat(appEngineLibrary.getLibraryFiles().size(), is(1));
    LibraryFile libraryFile = appEngineLibrary.getLibraryFiles().get(0);
    assertThat(libraryFile.getJavadocUri(), is(new URI("https://cloud.google.com/appengine/docs/java/javadoc/")));
    assertNull(libraryFile.getSourceUri());
    assertTrue(libraryFile.isExport());

    assertNotNull(libraryFile.getMavenCoordinates());
    MavenCoordinates mavenCoordinates = libraryFile.getMavenCoordinates();
    assertThat(mavenCoordinates.getRepository(), is("central"));
    assertThat(mavenCoordinates.getGroupId(), is("com.google.appengine"));
    assertThat(mavenCoordinates.getArtifactId(), is("appengine-api-1.0-sdk"));
    assertThat(mavenCoordinates.getVersion(), is("LATEST"));
    assertThat(mavenCoordinates.getType(), is("jar"));
    assertNull(mavenCoordinates.getClassifier());

    assertNotNull(libraryFile.getFilters());
    List<Filter> filters = libraryFile.getFilters();
    assertThat(filters.size(), is(4));
    assertTrue(filters.get(0).isExclude());
    assertThat(filters.get(0).getPattern(), is("com.google.appengine.repackaged.**"));
    assertTrue(filters.get(1).isExclude());
    assertThat(filters.get(1).getPattern(), is("com.google.appengine.labs.repackaged.**"));
    assertTrue(filters.get(2).isExclude());
    assertThat(filters.get(2).getPattern(), is("com.google.apphosting.**"));
    assertFalse(filters.get(3).isExclude());
    assertThat(filters.get(3).getPattern(), is("com.google.apphosting.api.**"));
  }
}
