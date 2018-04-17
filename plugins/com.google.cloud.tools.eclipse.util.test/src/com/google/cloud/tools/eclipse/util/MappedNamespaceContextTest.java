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
import java.util.Iterator;
import javax.xml.XMLConstants;
import org.hamcrest.Matchers;
import org.junit.Test;

public class MappedNamespaceContextTest {

  private static final MappedNamespaceContext sampleContext = new MappedNamespaceContext()
      .declareNamespace("p", "scheme://multiple/prefixes/")
      .declareNamespace("prefix", "scheme://multiple/prefixes/")
      .declareNamespace("maven", "http://maven.apache.org/POM/4.0.0");

  @Test
  public void testConstructor_nullPrefix() {
    try {
      new MappedNamespaceContext(null, "scheme://host/path/");
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("Prefix can't be null", ex.getMessage());
    }
  }

  @Test
  public void testConstructor_nullUri() {
    try {
      new MappedNamespaceContext("p", null);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("Namespace URI can't be null", ex.getMessage());
    }
  }

  @Test
  public void testDeclareNamespace_nullPrefix() {
    try {
      sampleContext.declareNamespace(null, "scheme://host/path/");
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("Prefix can't be null", ex.getMessage());
    }
  }

  @Test
  public void testDeclareNamespace_nullUri() {
    try {
      sampleContext.declareNamespace("p", null);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("Namespace URI can't be null", ex.getMessage());
    }
  }

  @Test
  public void testGetNamespaceUri_nullPrefix() {
    try {
      sampleContext.getNamespaceURI(null);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("Prefix can't be null", ex.getMessage());
    }
  }

  @Test
  public void testGetNamespaceUri() {
    assertEquals("http://maven.apache.org/POM/4.0.0", sampleContext.getNamespaceURI("maven"));
    assertEquals("scheme://multiple/prefixes/", sampleContext.getNamespaceURI("p"));
  }

  @Test
  public void testGetNamespaceUri_noMapping() {
    assertEquals(XMLConstants.NULL_NS_URI, sampleContext.getNamespaceURI("html"));
  }

  @Test
  public void testGetPrefix_nullUri() {
    try {
      sampleContext.getPrefix(null);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("Namespace URI can't be null", ex.getMessage());
    }
  }

  @Test
  public void testGetPrefix() {
    assertEquals("maven", sampleContext.getPrefix("http://maven.apache.org/POM/4.0.0"));
  }

  @Test
  public void testGetPrefix_noMapping() {
    assertNull(sampleContext.getPrefix("ftp://no/mapping"));
  }

  @Test
  public void testGetPrefixes_nullUri() {
    try {
      sampleContext.getPrefixes(null);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("Namespace URI can't be null", ex.getMessage());
    }
  }

  @Test
  public void testGetPrefixes() {
    Iterator<String> iterator = sampleContext.getPrefixes("scheme://multiple/prefixes/");
    ImmutableList<String> prefixes = ImmutableList.copyOf(iterator);
    assertThat(prefixes, Matchers.hasItem("p"));
    assertThat(prefixes, Matchers.hasItem("prefix"));
  }

  @Test
  public void testGetPrefixes_noMapping() {
    assertFalse(sampleContext.getPrefixes("ftp://no/mapping").hasNext());
  }

  @Test
  public void testGetPrefixes_immutable() {
    String mavenUri = "http://maven.apache.org/POM/4.0.0";
    Iterator<String> iterator = sampleContext.getPrefixes(mavenUri);

    assertTrue(iterator.hasNext());
    while (iterator.hasNext()) {
      iterator.next();
      iterator.remove();
    }

    assertEquals("maven", sampleContext.getPrefix(mavenUri));
  }

  @Test
  public void testGetNamespaceUri_xmlPrefix() {
    assertEquals("http://www.w3.org/XML/1998/namespace", sampleContext.getNamespaceURI("xml"));
  }

  @Test
  public void testGetPrefix_xmlNamespace() {
    assertEquals("xml", sampleContext.getPrefix("http://www.w3.org/XML/1998/namespace"));
  }

  @Test
  public void testConstructor_xmlPrefix() {
    try {
      new MappedNamespaceContext("xml", "http://example.com/");
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("Cannot redefine the 'xml' prefix", ex.getMessage());
    }
  }

  @Test
  public void testDeclareNamespace_xmlPrefix() {
    try {
      sampleContext.declareNamespace("xml", "http://example.com/");
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("Cannot redefine the 'xml' prefix", ex.getMessage());
    }
  }

  @Test
  public void testConstructor_noErrorWhenXmlPrefixWithCorrectUri() {
    MappedNamespaceContext context = new MappedNamespaceContext(
        "xml", "http://www.w3.org/XML/1998/namespace");
    assertEquals("http://www.w3.org/XML/1998/namespace", context.getNamespaceURI("xml"));
    assertEquals("xml", context.getPrefix("http://www.w3.org/XML/1998/namespace"));
  }

  @Test
  public void testDeclareNamespace_noErrorWhenXmlPrefixWithCorrectUri() {
    sampleContext.declareNamespace("xml", "http://www.w3.org/XML/1998/namespace");
    assertEquals("http://www.w3.org/XML/1998/namespace", sampleContext.getNamespaceURI("xml"));
    assertEquals("xml", sampleContext.getPrefix("http://www.w3.org/XML/1998/namespace"));
  }

  @Test
  public void testConstructor_xmlnsPrefix() {
    try {
      new MappedNamespaceContext("xmlns", "http://example.com/");
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("Cannot redefine the 'xmlns' prefix", ex.getMessage());
    }
  }

  @Test
  public void testDeclareNamespace_xmlnsPrefix() {
    try {
      sampleContext.declareNamespace("xmlns", "http://example.com/");
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("Cannot redefine the 'xmlns' prefix", ex.getMessage());
    }
  }

  @Test
  public void testConstructor_noErrorWhenXmlnsPrefixWithCorrectUri() {
    MappedNamespaceContext context = new MappedNamespaceContext(
        "xmlns", "http://www.w3.org/2000/xmlns/");
    assertEquals("http://www.w3.org/2000/xmlns/", context.getNamespaceURI("xmlns"));
    assertEquals("xmlns", context.getPrefix("http://www.w3.org/2000/xmlns/"));
  }

  @Test
  public void testDeclareNamespace_noErrorWhenXmlnsPrefixWithCorrectUri() {
    sampleContext.declareNamespace("xmlns", "http://www.w3.org/2000/xmlns/");
    assertEquals("http://www.w3.org/2000/xmlns/", sampleContext.getNamespaceURI("xmlns"));
    assertEquals("xmlns", sampleContext.getPrefix("http://www.w3.org/2000/xmlns/"));
  }

  @Test
  public void testConstructor_noErrorWhenXmlnsPrefixWithNullUri() {
    MappedNamespaceContext context = new MappedNamespaceContext("xmlns", "");
    assertEquals("", context.getNamespaceURI("xmlns"));
    assertEquals("xmlns", context.getPrefix(""));
  }

  @Test
  public void testDeclareNamespace_noErrorWhenXmlnsPrefixWithNulltUri() {
    sampleContext.declareNamespace("xmlns", "");
    assertEquals("", sampleContext.getNamespaceURI("xmlns"));
    assertEquals("xmlns", sampleContext.getPrefix(""));
  }
}
