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

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Locator2Impl;
import org.xml.sax.helpers.AttributesImpl;

public class WebXmlScannerTest {

  private WebXmlScanner scanner = new WebXmlScanner();
  
  @Before
  public void setUp() throws SAXException {
    scanner.setDocumentLocator(new Locator2Impl());
    scanner.startDocument();
  }
  
  @Test
  public void testStartElement_jcpNamespace() throws SAXException {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", "", "version", "", "3.1");
    scanner.startElement("http://xmlns.jcp.org/xml/ns/javaee", "web-app", "", attributes);
    assertEquals(1, scanner.getBlacklist().size());
    String message = scanner.getBlacklist().peek().getMessage();
    String expectedMessage = "App Engine Standard does not support this servlet version";
    assertEquals(expectedMessage, message);
  }
  
  @Test
  public void testStartElement_sunNamespace() throws SAXException {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", "", "version", "", "3.0");
    scanner.startElement("http://java.sun.com/xml/ns/javaee", "web-app", "", attributes);
    assertEquals(1, scanner.getBlacklist().size());
    String message = scanner.getBlacklist().peek().getMessage();
    String expectedMessage = "App Engine Standard does not support this servlet version";
    assertEquals(expectedMessage, message);
  }
  
  @Test
  public void testStartElement_correctVersion() throws SAXException {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", "", "version", "", "2.5");
    scanner.startElement("http://java.sun.com/xml/ns/javaee", "web-app", "", attributes);
    assertEquals(0, scanner.getBlacklist().size());
  }
  
}
