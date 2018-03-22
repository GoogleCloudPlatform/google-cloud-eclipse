/*
 * Copyright 2017 Google LLC. All Rights Reserved.
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
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PositionalXmlScannerTest {

  @Test
  public void testParse() throws SAXException, IOException {
    byte[] bytes = "<root><child></child></root>".getBytes(StandardCharsets.UTF_8);
    Document document = PositionalXmlScanner.parse(bytes);
    
    NodeList rootNode = document.getElementsByTagName("root");
    assertEquals(1, rootNode.getLength());
    
    NodeList childNode = document.getElementsByTagName("child");
    assertEquals(1, childNode.getLength());
  }
  
  @Test
  public void testParse_emptyXml() throws SAXException, IOException {
    byte[] bytes = "".getBytes(StandardCharsets.UTF_8);
    Document document = PositionalXmlScanner.parse(bytes);
    assertNull(document);
  }

}
