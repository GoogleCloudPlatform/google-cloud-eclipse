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

import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AppEngineWebXmlValidatorTest {
  
  @Test
  public void testCheckForElements() throws ParserConfigurationException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder  = builderFactory.newDocumentBuilder();
    Document document = documentBuilder.newDocument();
    
    Element element =
        document.createElementNS("http://appengine.google.com/ns/1.0", "application");
    element.setUserData("location", new DocumentLocation(3, 15), null);
    document.appendChild(element);
    
    AppEngineWebXmlValidator validator = new AppEngineWebXmlValidator();
    ArrayList<BannedElement> blacklist = validator.checkForElements(null, document);
    assertEquals(1, blacklist.size());
    String markerId = "com.google.cloud.tools.eclipse.appengine.validation.applicationMarker";
    assertEquals(markerId, blacklist.get(0).getMarkerId());
  }
}