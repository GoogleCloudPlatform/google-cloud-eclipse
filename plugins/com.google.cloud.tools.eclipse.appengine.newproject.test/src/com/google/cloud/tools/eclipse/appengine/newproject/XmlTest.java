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

package com.google.cloud.tools.eclipse.appengine.newproject;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// todo: extract common code into a utility class
public class XmlTest {
    
  private Document document;
	
  @Before
  public void setUp() throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    document = builder.parse(
        new File("../com.google.cloud.tools.eclipse.appengine.newproject/plugin.xml"));
  }
    
  @Test
  public void testPluginXml() {
    Assert.assertEquals("plugin", document.getDocumentElement().getNodeName());
  }
  
  @Test
  public void testValidExtensionPoints() {
    NodeList extensions = document.getDocumentElement().getElementsByTagName("extension");
    for (int i = 0; i < extensions.getLength(); i++) {
      Element extension = (Element) extensions.item(i);
      String point = extension.getAttribute("point");
      IExtensionRegistry registry = RegistryFactory.getRegistry();
      IExtensionPoint extensionPoint = registry.getExtensionPoint(point);
      Assert.assertNotNull(extensionPoint);
    }
  }
  
  
}
