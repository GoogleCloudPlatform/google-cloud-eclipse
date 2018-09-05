/*
 * Copyright 2016 Google LLC
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.runtime.CoreException;
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

  private final XPathFactory xpathFactory = XPathFactory.newInstance();

  @Before
  public void setUp() {
    doc = pluginXmlDocument.get();
  }

  protected final Document getDocument() {
    return doc;
  }

  /** Pull out the selected nodes from the given root. */
  protected NodeList findNodes(String xpathExpression, Element root) {
    try {
      XPath xpath = xpathFactory.newXPath();
      return (NodeList) xpath.evaluate(xpathExpression, root, XPathConstants.NODESET);
    } catch (XPathExpressionException ex) {
      throw new AssertionError(ex.toString(), ex);
    }
  }

  /** Pull out the selected nodes from the document. */
  protected NodeList findNodes(String xpathExpression) {
    return findNodes(xpathExpression, getDocument().getDocumentElement());
  }

  /** Return the single element matching the expression. */
  protected Element findElement(String xpathExpression, Element root) {
    NodeList list = findNodes(xpathExpression, root);
    assertEquals(1, list.getLength());
    return (Element) list.item(0);
  }

  /** Return the single element matching the expression. */
  protected Element findElement(String xpathExpression) {
    return findElement(xpathExpression, getDocument().getDocumentElement());
  }

  /** Return the extensions of the given extension point. */
  protected final NodeList getExtensions(String extensionPointId) {
    return findNodes("//plugin/extension[@point='" + extensionPointId + "']");
  }

  /** Verify that a node is a valid expression definition. */
  protected static Node checkExpressionDefinition(Element definition) {
    Assert.assertEquals("definition", definition.getNodeName());
    String id = definition.getAttribute("id");
    Assert.assertNotNull("definition must have an 'id' attribute", id);
    Assert.assertFalse("invalid definition ID", Strings.isNullOrEmpty(id));
    Assert.assertEquals(
        "definition should have only an 'id' attribute", 1, definition.getAttributes().getLength());
    List<Node> expressions = getChildNodes(definition, Node.ELEMENT_NODE);
    Assert.assertEquals("definition must have only 1 subexpression", 1, expressions.size());
    Assert.assertTrue(expressions.get(0) instanceof Element);
    checkExpression((Element) expressions.get(0));
    return definition;
  }

  /** Verify that a node is a valid expression. */
  protected static Expression checkExpression(Element expression) {
    try {
      Expression converted = ExpressionConverter.getDefault().perform(expression);
      Assert.assertNotNull(converted);
      return converted;
    } catch (CoreException ex) {
      throw new AssertionError("failed to convert to core expression", ex);
    }
  }

  /** Return the child nodes of given type. */
  protected static final List<Node> getChildNodes(Node parent, short nodeType) {
    List<Node> result = new ArrayList<>();
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == nodeType) {
        result.add(child);
      }
    }
    return result;
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
  public final void testBuildPropertiesContainsPluginFiles() {
    String[] binIncludes = buildProperties.get("bin.includes").split(",\\s*");
    Set<String> includes = Sets.newHashSet(binIncludes);
    
    Assert.assertTrue(includes.contains("plugin.xml"));
    Assert.assertTrue(includes.contains("plugin.properties"));
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
      Assert.assertNotNull(name + " is not defined", value);
      Assert.assertFalse(name + " is not defined", value.isEmpty());
    }
  }

  @Test
  public final void testPropertiesDefinedInManifestMf() throws IOException {
    boolean localizedMessageExists = false;

    Attributes attributes = getManifestAttributes();
    for (Object object : attributes.values()) {
      String value = object.toString();
      assertPropertyDefined(value);

      if (value.startsWith("%")) {
        localizedMessageExists = true;
      }
    }

    if (localizedMessageExists) {
      assertEquals("plugin", attributes.getValue("Bundle-Localization"));
    }
  }

  @Test
  public final void testBundleVendor() throws IOException {
    String vendor = getManifestAttributes().getValue("Bundle-Vendor");
    if (vendor.startsWith("%")) {
      vendor = pluginProperties.get(vendor.substring(1));
    }
    assertEquals("Google LLC", vendor);
  }
  
  @Test
  public final void testBundleActivationPolicyLazy() throws IOException {
    String policy = getManifestAttributes().getValue("Bundle-ActivationPolicy");
    assertEquals("lazy", policy);
  }
  
  @Test
  public final void testManifestVersion() throws IOException {
    Attributes manifest = getManifestAttributes();
    assertEquals("1.0", manifest.getValue("Manifest-Version"));
    assertEquals("2", manifest.getValue("Bundle-ManifestVersion"));
  }

  @Test
  public final void testBundleExecutionEnvironment() throws IOException {
    Attributes manifest = getManifestAttributes();
    assertEquals("JavaSE-1.8", manifest.getValue("Bundle-RequiredExecutionEnvironment"));
  }

  @Test
  public final void testGuavaImportVersions() throws IOException {
    checkDependencyDirectives(
        "Import-Package", "com.google.common.", "version=\"[25.1.0,26.0.0)\"");
    checkDependencyDirectives(
        "Require-Bundle", "com.google.guava", "bundle-version=\"[25.1.0,26.0.0)\"");
  }

  @Test
  public final void testGoogleApisImportVersions() throws IOException {
    checkDependencyDirectives(
        "Import-Package", "com.google.api.", "version=\"[1.23.0,1.24.0)\"");
  }

  @Test
  public final void testGoogleApisExportVersions() throws IOException {
    checkDependencyDirectives(
        "Export-Package", "com.google.api.", "version=\"1.23.0\"");
  }

  private void checkDependencyDirectives(
      String attributeName, String prefixToCheck, String versionString) throws IOException {
    String value = getManifestAttributes().getValue(attributeName);
    if (value != null) {
      String regexPrefix = prefixToCheck.replaceAll("\\.", "\\\\.");
      Pattern pattern = Pattern.compile(regexPrefix + "[^;,]*");

      Matcher matcher = pattern.matcher(value);
      while (matcher.find()) {
        int nextCharOffset = matcher.end();
        if (nextCharOffset == value.length()) {
          fail(attributeName + " directive not defined: " + matcher.group());
        }
        String stringAfterMatch = value.substring(nextCharOffset);
        if (!stringAfterMatch.startsWith(";" + versionString)) {
          fail(attributeName + " directive incorrect: " + matcher.group());
        }
      }
    }
  }

  private static Attributes getManifestAttributes() throws IOException {
    String bundlePath = EclipseProperties.getHostBundlePath();
    String manifestLocation = bundlePath + "/META-INF/MANIFEST.MF";

    try (InputStream in = Files.newInputStream(Paths.get(manifestLocation))) {
      return new Manifest(in).getMainAttributes();
    }
  }
}
