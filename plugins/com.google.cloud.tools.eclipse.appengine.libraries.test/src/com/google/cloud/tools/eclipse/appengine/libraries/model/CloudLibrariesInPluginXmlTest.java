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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Test the libraries defined in plugin.xml to validate that
 * their attributes are correctly set.
 */
public class CloudLibrariesInPluginXmlTest {

  private static final String APP_ENGINE_API_LIBRARY_ID = "appengine-api";
  private static final String CLOUD_ENDPOINTS_LIBRARY_ID = "appengine-endpoints";
  private static final String OBJECTIFY_LIBRARY_ID = "objectify";
  private static final String SERVLET_API_LIBRARY_ID = "servlet-api-2.5";
  private static final String JSP_API_LIBRARY_ID = "jsp-api-2.1";

  @Test
  public void testLibrarySize() {
    assertThat(CloudLibraries.getLibraries("appengine").size(), is(2));
    // There may be different number of servlet libraries depending on whether the
    // .appengine.java.standard.java8 bundle is present
    List<Library> servletLibraries = CloudLibraries.getLibraries("servlet");
    assertThat(servletLibraries, Matchers.hasItem(
        Matchers.<Library>hasToString("Library: id=servlet-api-2.5; name=Servlet API 2.5")));
    assertThat(servletLibraries, Matchers.hasItem(
        Matchers.<Library>hasToString("Library: id=jsp-api-2.1; name=Java Server Pages API 2.1")));
  }

  @Test
  public void testAppEngineApiLibraryConfig() throws URISyntaxException {
    Library appEngineLibrary = CloudLibraries.getLibrary(APP_ENGINE_API_LIBRARY_ID);
    assertThat(appEngineLibrary.getContainerPath().toString(),
               is(Library.CONTAINER_PATH_PREFIX + "/" + APP_ENGINE_API_LIBRARY_ID));
    assertThat(appEngineLibrary.getId(), is(APP_ENGINE_API_LIBRARY_ID));
    assertThat(appEngineLibrary.getName(), is("App Engine API"));
    assertThat(appEngineLibrary.getGroup(), is("appengine-api"));
    assertFalse(appEngineLibrary.getToolTip().isEmpty());
    assertThat(appEngineLibrary.getSiteUri(),
        is(new URI("https://cloud.google.com/appengine/docs/java/")));
    assertTrue(appEngineLibrary.isExport());

    assertThat(appEngineLibrary.getAllDependencies().size(), is(1));
    LibraryFile libraryFile = appEngineLibrary.getAllDependencies().get(0);
    assertThat(libraryFile.getJavadocUri(),
        is(new URI("https://cloud.google.com/appengine/docs/java/javadoc/")));
    assertNull(libraryFile.getSourceUri());
    assertFalse("App Engine API exported", libraryFile.isExport());

    assertNotNull(libraryFile.getMavenCoordinates());
    MavenCoordinates mavenCoordinates = libraryFile.getMavenCoordinates();
    assertThat(mavenCoordinates.getRepository(), is("central"));
    assertThat(mavenCoordinates.getGroupId(), is("com.google.appengine"));
    assertThat(mavenCoordinates.getArtifactId(), is("appengine-api-1.0-sdk"));
    assertThat(mavenCoordinates.getVersion(), is("1.9.57"));
    assertThat(mavenCoordinates.getType(), is("jar"));
    assertNull(mavenCoordinates.getClassifier());

    assertNotNull(libraryFile.getFilters());
    List<Filter> filters = libraryFile.getFilters();
    assertThat(filters.size(), is(4));
    assertTrue(filters.get(0).isExclude());
    assertThat(filters.get(0).getPattern(), is("com/google/appengine/repackaged/**"));
    assertTrue(filters.get(1).isExclude());
    assertThat(filters.get(1).getPattern(), is("com/google/appengine/labs/repackaged/**"));
    assertFalse(filters.get(2).isExclude());
    assertThat(filters.get(2).getPattern(), is("com/google/apphosting/api/**"));
    assertTrue(filters.get(3).isExclude());
    assertThat(filters.get(3).getPattern(), is("com/google/apphosting/**"));
  }

  @Test
  public void testEndpointsLibraryConfig() throws URISyntaxException {
    Library endpointsLibrary = CloudLibraries.getLibrary(CLOUD_ENDPOINTS_LIBRARY_ID);
    assertThat(endpointsLibrary.getContainerPath().toString(),
               is(Library.CONTAINER_PATH_PREFIX + "/" + CLOUD_ENDPOINTS_LIBRARY_ID));
    assertThat(endpointsLibrary.getId(), is(CLOUD_ENDPOINTS_LIBRARY_ID));
    assertThat(endpointsLibrary.getName(), is("Google Cloud Endpoints"));
    assertThat(endpointsLibrary.getGroup(), is("appengine"));
    assertThat(endpointsLibrary.getSiteUri(), is(new URI(
        "https://cloud.google.com/endpoints/docs/frameworks/java/about-cloud-endpoints-frameworks")));
    assertTrue(endpointsLibrary.isExport());
    assertTrue(endpointsLibrary.getToolTip().contains("v2"));

    assertThat(endpointsLibrary.getDirectDependencies().size(), is(1));
    LibraryFile libraryFile = endpointsLibrary.getDirectDependencies().get(0);
    assertNull(libraryFile.getSourceUri());
    assertTrue("Endpoints library not exported", libraryFile.isExport());

    assertNotNull(libraryFile.getMavenCoordinates());
    MavenCoordinates mavenCoordinates = libraryFile.getMavenCoordinates();
    assertThat(mavenCoordinates.getRepository(), is("central"));
    assertThat(mavenCoordinates.getGroupId(), is("com.google.endpoints"));
    assertThat(mavenCoordinates.getArtifactId(), is("endpoints-framework"));
    assertThat(mavenCoordinates.getVersion(), is("2.0.9"));
    assertThat(mavenCoordinates.getType(), is("jar"));
    assertNull(mavenCoordinates.getClassifier());
    assertThat(libraryFile.getJavadocUri(),
        is(new URI("https://cloud.google.com/endpoints/docs/frameworks/java/javadoc/")));

    assertNotNull(libraryFile.getFilters());
    List<Filter> filters = libraryFile.getFilters();
    assertThat(filters.size(), is(1));
    assertTrue(filters.get(0).isExclude());
    assertThat(filters.get(0).getPattern(), is("com/google/appengine/repackaged/**"));
  }

  @Test
  public void testObjectifyLibraryConfig() throws URISyntaxException {
    Library objectifyLibrary = CloudLibraries.getLibrary(OBJECTIFY_LIBRARY_ID);
    assertThat(objectifyLibrary.getContainerPath().toString(),
               is(Library.CONTAINER_PATH_PREFIX + "/" + OBJECTIFY_LIBRARY_ID));
    assertThat(objectifyLibrary.getId(), is(OBJECTIFY_LIBRARY_ID));
    assertThat(objectifyLibrary.getName(), is("Objectify"));
    assertThat(objectifyLibrary.getGroup(), is("appengine"));
    assertThat(objectifyLibrary.getSiteUri(),
        is(new URI("https://github.com/objectify/objectify/wiki")));
    assertTrue(objectifyLibrary.isExport());

    List<LibraryFile> allDependencies = objectifyLibrary.getAllDependencies();
    assertTrue(allDependencies.size() > 2);
    
    LibraryFile objectifyLibraryFile = null;
    LibraryFile guavaLibraryFile = null;
    for (LibraryFile file : allDependencies) {
      if (file.getMavenCoordinates().getArtifactId().equals("objectify")) {
        objectifyLibraryFile = file;
      } else if (file.getMavenCoordinates().getArtifactId().equals("guava")) {
        guavaLibraryFile = file;
      }
    }
    assertNotNull("objectify not found", objectifyLibraryFile);
    assertTrue("Objectify not exported", objectifyLibraryFile.isExport());
    assertNotNull("guava not found", guavaLibraryFile);

    MavenCoordinates objectifyMavenCoordinates = objectifyLibraryFile.getMavenCoordinates();
    assertThat(objectifyMavenCoordinates.getRepository(), is("central"));
    assertThat(objectifyMavenCoordinates.getGroupId(), is("com.googlecode.objectify"));
    assertThat(objectifyMavenCoordinates.getArtifactId(), is("objectify"));
    assertThat(objectifyMavenCoordinates.getVersion(), is("5.1.21"));
    assertThat(objectifyMavenCoordinates.getType(), is("jar"));
    assertNull(objectifyMavenCoordinates.getClassifier());

    assertNotNull(objectifyLibraryFile.getFilters());
    assertTrue(objectifyLibraryFile.getFilters().isEmpty());
    assertThat(objectifyLibraryFile.getJavadocUri(),
        is(new URI("https://www.javadoc.io/doc/com.googlecode.objectify/objectify/5.1.21")));
    
    assertNull(guavaLibraryFile.getSourceUri());
    assertTrue("Guava not exported", guavaLibraryFile.isExport());

    MavenCoordinates guavaMavenCoordinates = guavaLibraryFile.getMavenCoordinates();
    assertThat(guavaMavenCoordinates.getRepository(), is("central"));
    assertThat(guavaMavenCoordinates.getGroupId(), is("com.google.guava"));
    assertThat(guavaMavenCoordinates.getArtifactId(), is("guava"));
    assertThat(guavaMavenCoordinates.getVersion(), is("20.0"));
    assertThat(guavaMavenCoordinates.getType(), is("jar"));
    assertNull(guavaMavenCoordinates.getClassifier());

    assertNotNull(guavaLibraryFile.getFilters());
    assertTrue(guavaLibraryFile.getFilters().isEmpty());
  }

  @Test
  public void testServletApiLibraryConfig() throws URISyntaxException {
    Library servletApiLibrary = CloudLibraries.getLibrary(SERVLET_API_LIBRARY_ID);
    assertThat(servletApiLibrary.getContainerPath().toString(),
               is(Library.CONTAINER_PATH_PREFIX + "/" + SERVLET_API_LIBRARY_ID));
    assertThat(servletApiLibrary.getId(), is(SERVLET_API_LIBRARY_ID));
    assertThat(servletApiLibrary.getName(), is("Servlet API 2.5"));
    assertThat(servletApiLibrary.getGroup(), is("servlet"));
    assertThat(servletApiLibrary.getSiteUri(),
        is(new URI("http://www.oracle.com/technetwork/java/javaee/servlet/index.html")));
    assertFalse(servletApiLibrary.isExport());
    assertNotNull(servletApiLibrary.getLibraryDependencies());
    assertTrue(servletApiLibrary.getLibraryDependencies().isEmpty());

    assertThat(servletApiLibrary.getAllDependencies().size(), is(1));
    LibraryFile libraryFile = servletApiLibrary.getAllDependencies().get(0);
    assertThat(libraryFile.getJavadocUri(), is(new URI(
        "https://docs.oracle.com/cd/E17802_01/products/products/servlet/2.5/docs/servlet-2_5-mr2/")));
    assertNull(libraryFile.getSourceUri());

    assertNotNull(libraryFile.getMavenCoordinates());
    MavenCoordinates mavenCoordinates = libraryFile.getMavenCoordinates();
    assertThat(mavenCoordinates.getRepository(), is("central"));
    assertThat(mavenCoordinates.getGroupId(), is("javax.servlet"));
    assertThat(mavenCoordinates.getArtifactId(), is("servlet-api"));
    assertThat(mavenCoordinates.getVersion(), is("2.5"));
    assertThat(mavenCoordinates.getType(), is("jar"));
    assertNull(mavenCoordinates.getClassifier());

    assertNotNull(libraryFile.getFilters());
    assertTrue(libraryFile.getFilters().isEmpty());
  }

  @Test
  public void testJspApiLibraryConfig() throws URISyntaxException {
    Library jspApiLibrary = CloudLibraries.getLibrary(JSP_API_LIBRARY_ID);
    assertThat(jspApiLibrary.getContainerPath().toString(),
               is(Library.CONTAINER_PATH_PREFIX + "/" + JSP_API_LIBRARY_ID));
    assertThat(jspApiLibrary.getId(), is(JSP_API_LIBRARY_ID));
    assertThat(jspApiLibrary.getGroup(), is("servlet"));
    assertThat(jspApiLibrary.getName(), is("Java Server Pages API 2.1"));
    assertThat(jspApiLibrary.getSiteUri(),
        is(new URI("http://www.oracle.com/technetwork/java/javaee/jsp/index.html")));
    assertFalse(jspApiLibrary.isExport());
    assertNotNull(jspApiLibrary.getLibraryDependencies());
    assertTrue(jspApiLibrary.getLibraryDependencies().isEmpty());

    assertThat(jspApiLibrary.getAllDependencies().size(), is(1));
    LibraryFile jspApi = jspApiLibrary.getAllDependencies().get(0);
    assertThat(jspApi.getJavadocUri(), is(new URI(
        "http://docs.oracle.com/cd/E17802_01/products/products/jsp/2.1/docs/jsp-2_1-pfd2/")));
    assertNull(jspApi.getSourceUri());

    assertNotNull(jspApi.getMavenCoordinates());
    MavenCoordinates mavenCoordinates = jspApi.getMavenCoordinates();
    assertThat(mavenCoordinates.getRepository(), is("central"));
    assertThat(mavenCoordinates.getGroupId(), is("javax.servlet.jsp"));
    assertThat(mavenCoordinates.getArtifactId(), is("jsp-api"));
    assertThat(mavenCoordinates.getVersion(), is("2.1"));
    assertThat(mavenCoordinates.getType(), is("jar"));
    assertNull(mavenCoordinates.getClassifier());

    assertTrue(jspApi.getFilters().isEmpty());
  }
}
