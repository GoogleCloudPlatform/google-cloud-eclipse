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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

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

import com.google.common.collect.Sets;

/**
 * Generic tests that should be true of all plugins.
 */
@SuppressWarnings("restriction")
public abstract class BasePluginXmlTest {

  @Rule
  public final PluginXmlDocument pluginXmlDocument = new PluginXmlDocument();
  @Rule
  public final EclipseProperties pluginProperties = new EclipseProperties("plugin.properties");
  @Rule
  public final EclipseProperties buildProperties = new EclipseProperties("build.properties");
  
  private Document doc;

  @Before
  public void setUp() {
    doc = pluginXmlDocument.get();
  }

  protected final Document getDocument() {
    return doc;
  } 
  
  @Test
  public final void testRootElementIsPlugin() {
    Assert.assertEquals("plugin", getDocument().getDocumentElement().getNodeName());
  }
   
  @Test
  public final void testValidExtensionPoints() {
    NodeList extensions = getDocument().getElementsByTagName("extension");
    Assert.assertTrue(
        "plugin.xml must contain at least one extension point", extensions.getLength() > 0);
        
    IExtensionRegistry registry = RegistryFactory.getRegistry();
    Assert.assertNotNull("Make sure you're running this as a plugin test", registry);
    for (int i = 0; i < extensions.getLength(); i++) {
      Element extension = (Element) extensions.item(i);
      String point = extension.getAttribute("point");
      Assert.assertNotNull("Could not load " + extension.getAttribute("id"), point);
      IExtensionPoint extensionPoint = registry.getExtensionPoint(point);
      Assert.assertNotNull("Could not load " + extension.getAttribute("id"), extensionPoint);
    }
  }
  
  @Test
  public final void testKeywordsDefined() {
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
  public final void testBuildProperties() throws IOException {
    String[] binIncludes = buildProperties.get("bin.includes").split(",\\s*");
    Set<String> includes = Sets.newHashSet(binIncludes);
    
    Assert.assertTrue(includes.contains("plugin.xml"));
    Assert.assertTrue(includes.contains("plugin.properties"));
    Assert.assertTrue(includes.contains("."));
    Assert.assertTrue(includes.contains("META-INF/"));
    
    testIncludedIfPresent(includes, "helpContexts.xml");
    testIncludedIfPresent(includes, "icons/");
    testIncludedIfPresent(includes, "lib/");
    testIncludedIfPresent(includes, "README.md");
    testIncludedIfPresent(includes, "epl-v10.html");
    testIncludedIfPresent(includes, "OSGI-INF/");
    testIncludedIfPresent(includes, "fragment.xml");
    testIncludedIfPresent(includes, "fragment.properties");
  }

  private static void testIncludedIfPresent(Set<String> includes, String name) 
      throws IOException {
    String path = EclipseProperties.getHostBundlePath() + "/" + name;
    if (Files.exists(Paths.get(path))) {
      Assert.assertTrue(includes.contains(name));
    }
  }
  
  @Test
  public final void testPropertiesDefined() {
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
      String value = pluginProperties.get(name.substring(1));
      Assert.assertNotNull(name + " is not defined");
      Assert.assertFalse(name + " is not defined", value.isEmpty());
    }
  }

}
