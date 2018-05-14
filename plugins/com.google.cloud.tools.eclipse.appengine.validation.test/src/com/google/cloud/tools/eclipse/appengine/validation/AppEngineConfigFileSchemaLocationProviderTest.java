/*
 * Copyright 2018 Google LLC
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

package com.google.cloud.tools.eclipse.appengine.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.util.Map;
import org.eclipse.wst.xml.core.contentmodel.modelquery.IExternalSchemaLocationProvider;
import org.junit.Test;

/**
 * Test the generated schema locations.
 */
public class AppEngineConfigFileSchemaLocationProviderTest {
  private final AppEngineConfigFileSchemaLocationProvider fixture =
      new AppEngineConfigFileSchemaLocationProvider();

  @Test
  public void testAppEngineWebXml() {
    Map<String, String> locations =
        fixture.getExternalSchemaLocation(URI.create("/foo/WEB-INF/appengine-web.xml"));
    assertNotNull(locations);
    assertEquals(2, locations.size());
    assertEquals("platform:/plugin/com.google.cloud.tools.eclipse.appengine.validation/xsd/appengine-web.xsd",
        locations.get(IExternalSchemaLocationProvider.NO_NAMESPACE_SCHEMA_LOCATION));
    assertEquals("http://appengine.google.com/ns/1.0 platform:/plugin/com.google.cloud.tools.eclipse.appengine.validation/xsd/appengine-web.xsd",
        locations.get(IExternalSchemaLocationProvider.SCHEMA_LOCATION));
  }

  @Test
  public void testQueueXml() {
    assertNoNamespaceSchemaLocation(
        "platform:/plugin/com.google.cloud.tools.eclipse.appengine.validation/xsd/queue.xsd",
        fixture.getExternalSchemaLocation(URI.create("/foo/WEB-INF/queue.xml")));
  }

  @Test
  public void testDispatchXml() {
    assertNoNamespaceSchemaLocation(
        "platform:/plugin/com.google.cloud.tools.eclipse.appengine.validation/xsd/dispatch.xsd",
        fixture.getExternalSchemaLocation(URI.create("/foo/WEB-INF/dispatch.xml")));
  }

  @Test
  public void testDatastoreIndexesXml() {
    assertNoNamespaceSchemaLocation(
        "platform:/plugin/com.google.cloud.tools.eclipse.appengine.validation/xsd/datastore-indexes.xsd",
        fixture.getExternalSchemaLocation(URI.create("/foo/WEB-INF/datastore-indexes.xml")));
  }

  @Test
  public void testDatastoreIndexesAutoXml() {
    assertNoNamespaceSchemaLocation(
        "platform:/plugin/com.google.cloud.tools.eclipse.appengine.validation/xsd/datastore-indexes.xsd",
        fixture.getExternalSchemaLocation(URI.create("/foo/WEB-INF/datastore-indexes-auto.xml")));
  }

  @Test
  public void testDosXml() {
    assertNoNamespaceSchemaLocation(
        "platform:/plugin/com.google.cloud.tools.eclipse.appengine.validation/xsd/dos.xsd",
        fixture.getExternalSchemaLocation(URI.create("/foo/WEB-INF/dos.xml")));
  }

  @Test
  public void testNoFile() {
    Map<String, String> locations = fixture.getExternalSchemaLocation(URI.create("/"));
    assertNotNull(locations);
    assertEquals(0, locations.size());
  }

  @Test
  public void testUnknownFile() {
    Map<String, String> locations =
        fixture.getExternalSchemaLocation(URI.create("/foo/WEB-INF/foobar.xml"));
    assertNotNull(locations);
    assertEquals(0, locations.size());
  }

  private void assertNoNamespaceSchemaLocation(String expected,
      Map<String, String> externalSchemaLocationMap) {
    assertNotNull(externalSchemaLocationMap);
    assertEquals(1, externalSchemaLocationMap.size());
    assertEquals(expected, externalSchemaLocationMap
        .get(IExternalSchemaLocationProvider.NO_NAMESPACE_SCHEMA_LOCATION));
  }
}
