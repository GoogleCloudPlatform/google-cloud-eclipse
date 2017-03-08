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

package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Filter;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryRecommendation;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.junit.Test;

/**
 * Test the App Engine libraries defined in plugin.xml to validate that
 * their attributes are correctly set.
 */
public class AppEngineLibrariesInPluginXmlTest {

  private static final String APP_ENGINE_API_LIBRARY_ID = "appengine-api";
  private static final String CLOUD_ENDPOINTS_LIBRARY_ID = "appengine-endpoints";
  private static final String OBJECTIFY_LIBRARY_ID = "objectify";
  private static final String SERVLET_API_LIBRARY_ID = "servlet-api";
  private static final String JSP_API_LIBRARY_ID = "jsp-api";

  @Test
  public void testLibrarySize() {
    assertThat(AppEngineLibraries.getLibraries("appengine").size(), is(3));
    assertThat(AppEngineLibraries.getLibraries("servlet").size(), is(2));
  }

  @Test
  public void testAppEngineApiLibraryConfig() throws URISyntaxException {
    Library appEngineLibrary = AppEngineLibraries.getLibrary(APP_ENGINE_API_LIBRARY_ID);
    assertThat(appEngineLibrary.getContainerPath().toString(),
               is(Library.CONTAINER_PATH_PREFIX + "/" + APP_ENGINE_API_LIBRARY_ID));
    assertThat(appEngineLibrary.getId(), is(APP_ENGINE_API_LIBRARY_ID));
    assertThat(appEngineLibrary.getName(), is("App Engine API"));
    assertThat(appEngineLibrary.getGroup(), is("appengine"));
    assertFalse(appEngineLibrary.getToolTip().isEmpty());
    assertThat(appEngineLibrary.getSiteUri(), 
        is(new URI("https://cloud.google.com/appengine/docs/java/")));
    assertTrue(appEngineLibrary.isExport());

    assertThat(appEngineLibrary.getLibraryFiles().size(), is(1));
    LibraryFile libraryFile = appEngineLibrary.getLibraryFiles().get(0);
    assertThat(libraryFile.getJavadocUri(), 
        is(new URI("https://cloud.google.com/appengine/docs/java/javadoc/")));
    assertNull(libraryFile.getSourceUri());
    assertTrue("App Engine API not exported", libraryFile.isExport());

    assertNotNull(libraryFile.getMavenCoordinates());
    MavenCoordinates mavenCoordinates = libraryFile.getMavenCoordinates();
    assertThat(mavenCoordinates.getRepository(), is("central"));
    assertThat(mavenCoordinates.getGroupId(), is("com.google.appengine"));
    assertThat(mavenCoordinates.getArtifactId(), is("appengine-api-1.0-sdk"));
    assertThat(mavenCoordinates.getVersion(), is("1.9.50"));
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
    Library endpointsLibrary = AppEngineLibraries.getLibrary(CLOUD_ENDPOINTS_LIBRARY_ID);
    assertThat(endpointsLibrary.getContainerPath().toString(),
               is(Library.CONTAINER_PATH_PREFIX + "/" + CLOUD_ENDPOINTS_LIBRARY_ID));
    assertThat(endpointsLibrary.getId(), is(CLOUD_ENDPOINTS_LIBRARY_ID));
    assertThat(endpointsLibrary.getName(), is("Google Cloud Endpoints"));
    assertThat(endpointsLibrary.getGroup(), is("appengine"));
    assertThat(endpointsLibrary.getSiteUri(),
        is(new URI("https://cloud.google.com/appengine/docs/java/endpoints/")));
    assertTrue(endpointsLibrary.isExport());
    assertNotNull(endpointsLibrary.getLibraryDependencies());
    assertThat(endpointsLibrary.getLibraryDependencies().size(), is(1));
    assertThat(endpointsLibrary.getLibraryDependencies().get(0), is("appengine-api"));

    assertThat(endpointsLibrary.getLibraryFiles().size(), is(1));
    LibraryFile libraryFile = endpointsLibrary.getLibraryFiles().get(0);
    assertNull(libraryFile.getSourceUri());
    assertTrue("Endpoints library not exported", libraryFile.isExport());

    assertNotNull(libraryFile.getMavenCoordinates());
    MavenCoordinates mavenCoordinates = libraryFile.getMavenCoordinates();
    assertThat(mavenCoordinates.getRepository(), is("central"));
    assertThat(mavenCoordinates.getGroupId(), is("com.google.appengine"));
    assertThat(mavenCoordinates.getArtifactId(), is("appengine-endpoints"));
    assertThat(mavenCoordinates.getVersion(), is("1.9.50"));
    assertThat(mavenCoordinates.getType(), is("jar"));
    assertNull(mavenCoordinates.getClassifier());
    assertThat(libraryFile.getJavadocUri(), 
        is(new URI("https://cloud.google.com/appengine/docs/java/endpoints/javadoc/")));

    assertNotNull(libraryFile.getFilters());
    List<Filter> filters = libraryFile.getFilters();
    assertThat(filters.size(), is(1));
    assertTrue(filters.get(0).isExclude());
    assertThat(filters.get(0).getPattern(), is("com/google/appengine/repackaged/**"));
  }

  @Test
  public void testObjectifyLibraryConfig() throws URISyntaxException {
    Library objectifyLibrary = AppEngineLibraries.getLibrary(OBJECTIFY_LIBRARY_ID);
    assertThat(objectifyLibrary.getContainerPath().toString(),
               is(Library.CONTAINER_PATH_PREFIX + "/" + OBJECTIFY_LIBRARY_ID));
    assertThat(objectifyLibrary.getId(), is(OBJECTIFY_LIBRARY_ID));
    assertThat(objectifyLibrary.getName(), is("Objectify"));
    assertThat(objectifyLibrary.getGroup(), is("appengine"));
    assertThat(objectifyLibrary.getSiteUri(), 
        is(new URI("https://github.com/objectify/objectify/wiki")));
    assertTrue(objectifyLibrary.isExport());

    assertThat(objectifyLibrary.getLibraryFiles().size(), is(2));
    LibraryFile objectifyLibraryFile = objectifyLibrary.getLibraryFiles().get(0);
    assertNull(objectifyLibraryFile.getSourceUri());
    assertTrue("Objectify not exported", objectifyLibraryFile.isExport());

    assertNotNull(objectifyLibraryFile.getMavenCoordinates());
    MavenCoordinates objectifyMavenCoordinates = objectifyLibraryFile.getMavenCoordinates();
    assertThat(objectifyMavenCoordinates.getRepository(), is("central"));
    assertThat(objectifyMavenCoordinates.getGroupId(), is("com.googlecode.objectify"));
    assertThat(objectifyMavenCoordinates.getArtifactId(), is("objectify"));
    assertThat(objectifyMavenCoordinates.getVersion(), is("5.1.15"));
    assertThat(objectifyMavenCoordinates.getType(), is("jar"));
    assertNull(objectifyMavenCoordinates.getClassifier());

    assertNotNull(objectifyLibraryFile.getFilters());
    assertTrue(objectifyLibraryFile.getFilters().isEmpty());
    assertThat(objectifyLibraryFile.getJavadocUri(), 
        is(new URI("http://static.javadoc.io/com.googlecode.objectify/objectify/5.1.14/")));
    
    LibraryFile guavaLibraryFile = objectifyLibrary.getLibraryFiles().get(1);
    assertThat(guavaLibraryFile.getJavadocUri(), 
        is(new URI("https://google.github.io/guava/releases/20.0/api/docs/")));
    assertNull(guavaLibraryFile.getSourceUri());
    assertTrue("Guava not exported", guavaLibraryFile.isExport());

    assertNotNull(guavaLibraryFile.getMavenCoordinates());
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
    Library servletApiLibrary = AppEngineLibraries.getLibrary(SERVLET_API_LIBRARY_ID);
    assertThat(servletApiLibrary.getContainerPath().toString(),
               is(Library.CONTAINER_PATH_PREFIX + "/" + SERVLET_API_LIBRARY_ID));
    assertThat(servletApiLibrary.getId(), is(SERVLET_API_LIBRARY_ID));
    assertThat(servletApiLibrary.getName(), is("Servlet API 2.5"));
    assertThat(servletApiLibrary.getGroup(), is("servlet"));
    assertThat(servletApiLibrary.getSiteUri(), 
        is(new URI("http://www.oracle.com/technetwork/java/javaee/servlet/index.html")));
    assertFalse(servletApiLibrary.isExport());
    assertThat(servletApiLibrary.getRecommendation(), is(LibraryRecommendation.REQUIRED));
    assertNotNull(servletApiLibrary.getLibraryDependencies());
    assertTrue(servletApiLibrary.getLibraryDependencies().isEmpty());

    assertThat(servletApiLibrary.getLibraryFiles().size(), is(1));
    LibraryFile libraryFile = servletApiLibrary.getLibraryFiles().get(0);
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
    Library jspApiLibrary = AppEngineLibraries.getLibrary(JSP_API_LIBRARY_ID);
    assertThat(jspApiLibrary.getContainerPath().toString(),
               is(Library.CONTAINER_PATH_PREFIX + "/" + JSP_API_LIBRARY_ID));
    assertThat(jspApiLibrary.getId(), is(JSP_API_LIBRARY_ID));
    assertThat(jspApiLibrary.getGroup(), is("servlet"));
    assertThat(jspApiLibrary.getName(), is("Java Server Pages API 2.1"));
    assertThat(jspApiLibrary.getSiteUri(), 
        is(new URI("http://www.oracle.com/technetwork/java/javaee/jsp/index.html")));
    assertFalse(jspApiLibrary.isExport());
    assertThat(jspApiLibrary.getRecommendation(), is(LibraryRecommendation.OPTIONAL));
    assertNotNull(jspApiLibrary.getLibraryDependencies());
    assertTrue(jspApiLibrary.getLibraryDependencies().isEmpty());

    assertThat(jspApiLibrary.getLibraryFiles().size(), is(2));
    LibraryFile jspApi = jspApiLibrary.getLibraryFiles().get(0);
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
    
    LibraryFile jstlApi = jspApiLibrary.getLibraryFiles().get(1);
    assertThat(jstlApi.getJavadocUri(), is(new URI("https://jstl.java.net/")));
    assertNull(jstlApi.getSourceUri());

    assertNotNull(jstlApi.getMavenCoordinates());
    MavenCoordinates jstlCoordinates = jstlApi.getMavenCoordinates();
    assertThat(jstlCoordinates.getRepository(), is("central"));
    assertThat(jstlCoordinates.getGroupId(), is("javax.servlet"));
    assertThat(jstlCoordinates.getArtifactId(), is("jstl"));
    assertThat(jstlCoordinates.getVersion(), is("1.2"));
    assertThat(jstlCoordinates.getType(), is("jar"));
    assertNull(jstlCoordinates.getClassifier());

    assertTrue(jstlApi.getFilters().isEmpty());
  }

}
