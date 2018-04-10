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

package com.google.cloud.tools.eclipse.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.XMLConstants;
import org.hamcrest.Matchers;
import org.junit.Test;

public class MappedNamespaceContextTest {

  private static final MappedNamespaceContext SAMPLE_CONTEXT = new MappedNamespaceContext(
      ImmutableMap.of(
          "p", "scheme://multiple/prefixes/",
          "prefix", "scheme://multiple/prefixes/",
          "maven", "http://maven.apache.org/POM/4.0.0"));

  @Test
  public void testConstructor_singleMappingNullPrefix() {
    try {
      new MappedNamespaceContext(null, "scheme://host/path/");
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("prefix and URI can't be null", ex.getMessage());
    }
  }

  @Test
  public void testConstructor_singleMappingNullUri() {
    try {
      new MappedNamespaceContext("p", null);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("prefix and URI can't be null", ex.getMessage());
    }
  }

  @Test
  public void testConstructor_nullPrefix() {
    try {
      Map<String, String> map = new HashMap<>();
      map.put(null, "scheme://host/path/");
      new MappedNamespaceContext(map);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("prefix and URI can't be null", ex.getMessage());
    }
  }

  @Test
  public void testConstructor_nullUri() {
    try {
      Map<String, String> map = new HashMap<>();
      map.put("p", null);
      new MappedNamespaceContext(map);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("prefix and URI can't be null", ex.getMessage());
    }
  }

  @Test
  public void testGetNamespaceUri_nullPrefix() {
    try {
      SAMPLE_CONTEXT.getNamespaceURI(null);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("prefix can't be null", ex.getMessage());
    }
  }

  @Test
  public void testGetNamespaceUri() {
    assertEquals("http://maven.apache.org/POM/4.0.0", SAMPLE_CONTEXT.getNamespaceURI("maven"));
    assertEquals("scheme://multiple/prefixes/", SAMPLE_CONTEXT.getNamespaceURI("p"));
  }

  @Test
  public void testGetNamespaceUri_noMapping() {
    assertEquals(XMLConstants.NULL_NS_URI, SAMPLE_CONTEXT.getNamespaceURI("html"));
  }

  @Test
  public void testGetPrefix_nullUri() {
    try {
      SAMPLE_CONTEXT.getPrefix(null);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("namespaceUri can't be null", ex.getMessage());
    }
  }

  @Test
  public void testGetPrefix() {
    assertEquals("maven", SAMPLE_CONTEXT.getPrefix("http://maven.apache.org/POM/4.0.0"));
  }

  @Test
  public void testGetPrefix_noMapping() {
    assertNull(SAMPLE_CONTEXT.getPrefix("ftp://no/mapping"));
  }

  @Test
  public void testGetPrefixes_nullUri() {
    try {
      SAMPLE_CONTEXT.getPrefixes(null);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("namespaceUri can't be null", ex.getMessage());
    }
  }

  @Test
  public void testGetPrefixes() {
    Iterator<String> iterator = SAMPLE_CONTEXT.getPrefixes("scheme://multiple/prefixes/");
    ImmutableList<String> prefixes = ImmutableList.copyOf(iterator);
    assertThat(prefixes, Matchers.hasItem("p"));
    assertThat(prefixes, Matchers.hasItem("prefix"));
  }

  @Test
  public void testGetPrefixes_noMapping() {
    assertFalse(SAMPLE_CONTEXT.getPrefixes("ftp://no/mapping").hasNext());
  }

  @Test
  public void testGetPrefixes_immutable() {
    String mavenUri = "http://maven.apache.org/POM/4.0.0";
    Iterator<String> iterator = SAMPLE_CONTEXT.getPrefixes(mavenUri);

    assertTrue(iterator.hasNext());
    while (iterator.hasNext()) {
      iterator.next();
      iterator.remove();
    }

    assertEquals("maven", SAMPLE_CONTEXT.getPrefix(mavenUri));
  }

  @Test
  public void testConstructor_snapshotsMap() {
    Map<String, String> map = new HashMap<>();
    map.put("echo", "echo://echo/uri");
    MappedNamespaceContext context = new MappedNamespaceContext(map);
    map.clear();  // clearing after constructing the context

    assertEquals("echo", context.getPrefix("echo://echo/uri"));
  }
}
