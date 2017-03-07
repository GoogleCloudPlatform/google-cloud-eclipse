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

package com.google.cloud.tools.eclipse.appengine.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Locator2Impl;
import org.xml.sax.helpers.AttributesImpl;

public class PomXmlScannerTest {
  
  private static final String ELEMENT_MESSAGE = Messages.getString("maven.plugin");
  private PomXmlScanner scanner = new PomXmlScanner();
  
  @Before
  public void setUp() throws SAXException {
    scanner.setDocumentLocator(new Locator2Impl());
    scanner.startDocument();
  }
  
  @Test
  public void testStartElement_build() throws SAXException {
    scanner.startElement("", "plugin", "", new AttributesImpl());
    assertEquals(0, scanner.getBlacklist().size());
    assertTrue(scanner.getInsidePlugin());
  }
  
  @Test
  public void testStartElement_groupId() throws SAXException {
    scanner.startElement("", "plugin", "", new AttributesImpl());
    scanner.startElement("", "groupId", "", new AttributesImpl());
    assertTrue(scanner.getSaveContents());
  }
  
  @Test
  public void testStartElement_groupIdNoParent() throws SAXException {
    scanner.startElement("", "groupId", "", new AttributesImpl());
    assertFalse(scanner.getSaveContents());
  }
  
  @Test
  public void testEndElement() throws SAXException {
    scanner.startElement("", "plugin", "", new AttributesImpl());
    scanner.startElement("", "groupId", "", new AttributesImpl());
    scanner.characters("com.google.appengine".toCharArray(), 0, 20);
    scanner.endElement("", "groupId", "");
    assertFalse(scanner.getSaveContents());
    scanner.startElement("", "artifactId", "", new AttributesImpl());
    scanner.characters("appengine-maven-plugin".toCharArray(), 0, 22);
    scanner.endElement("", "artifactId", "");
    assertEquals(1, scanner.getBlacklist().size());
    String message = scanner.getBlacklist().peek().getMessage();
    assertEquals(ELEMENT_MESSAGE, message);
  }
  
  @Test
  public void testEndElement_gcloudPlugin() throws SAXException {
    scanner.startElement("", "plugin", "", new AttributesImpl());
    scanner.startElement("", "groupId", "", new AttributesImpl());
    String groupId = "com.google.appengine";
    scanner.characters(groupId.toCharArray(), 0, groupId.length());
    scanner.endElement("", "groupId", "");
    assertFalse(scanner.getSaveContents());
    
    scanner.startElement("", "artifactId", "", new AttributesImpl());
    String artifactId = "gcloud-maven-plugin";
    scanner.characters(artifactId.toCharArray(), 0, artifactId.length());
    scanner.endElement("", "artifactId", "");
    assertEquals(1, scanner.getBlacklist().size());
    
    String message = scanner.getBlacklist().peek().getMessage();
    assertEquals(ELEMENT_MESSAGE, message);
  }
  
}
