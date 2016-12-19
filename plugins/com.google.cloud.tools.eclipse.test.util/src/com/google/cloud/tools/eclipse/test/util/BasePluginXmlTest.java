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

package com.google.cloud.tools.eclipse.test.util;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.ui.internal.registry.KeywordRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("restriction")
public class BasePluginXmlTest {

  @Rule public final PluginXmlDocument pluginXmlDocument = new PluginXmlDocument();
  @Rule public final PluginProperties pluginProperties = new PluginProperties();
  
  private Document doc;

  @Before
  public void setUp() {
    doc = pluginXmlDocument.get();
  }

  protected Document getDocument() {
    return doc;
  }
  
  // Generic tests that should be true of all plugin.xml files
  
  @Test
  public void testRootElementIsPlugin() {
    Assert.assertEquals("plugin", getDocument().getDocumentElement().getNodeName());
  }
   
  @Test
  public void testValidExtensionPoints() {
    NodeList extensions = getDocument().getElementsByTagName("extension");
    
    // todo should we test that the file has at least one extension point?
    
    IExtensionRegistry registry = RegistryFactory.getRegistry();
    for (int i = 0; i < extensions.getLength(); i++) {
      Element extension = (Element) extensions.item(i);
      String point = extension.getAttribute("point");
      IExtensionPoint extensionPoint = registry.getExtensionPoint(point);
      Assert.assertNotNull(extensionPoint);
    }
  }
  
  @Test
  public void testKeywordsDefined() {
    NodeList references = getDocument().getElementsByTagName("keywordReference");
        
    if (references.getLength() > 0) { // not all files reference keywords
      KeywordRegistry registry = KeywordRegistry.getInstance();
      for (int i = 0; i < references.getLength(); i++) {
        Element reference = (Element) references.item(i);
        String id = reference.getAttribute("id");
        String keyword = registry.getKeywordLabel(id);
        Assert.assertNotNull("Null keyword " + id, keyword);
        Assert.assertFalse("Empty keyword " + id, keyword.isEmpty());
      }
    }
  }
  
  @Test
  public void testPropertiesDefined() {
    assertPropertiesDefined(getDocument().getDocumentElement());
  }
  
  private void assertPropertiesDefined(Element element) {
    NamedNodeMap attributes = element.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      String name = attributes.item(i).getNodeValue();
      assertPropertyDefined(name);
    }
    
    assertPropertyDefined(element.getTextContent());
    
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
        assertPropertiesDefined((Element) children.item(i));
      }
    }
  }

  private void assertPropertyDefined(String name) {
    if (name.startsWith("%")) {
      String value = pluginProperties.get().getProperty(name.substring(1));
      Assert.assertNotNull(name + " is not defined");
      Assert.assertFalse(name + " is not defined", value.isEmpty());
    }
  }

}
