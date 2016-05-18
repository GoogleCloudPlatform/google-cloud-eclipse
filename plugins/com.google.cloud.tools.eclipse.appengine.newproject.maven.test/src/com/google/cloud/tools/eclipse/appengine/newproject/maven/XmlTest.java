package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

// todo: extract common code into a utility class
public class XmlTest {

  private Document parse(File location)
      throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(location);
  }

  @Test
  public void testPluginXml() throws Exception {
    Document document =
        parse(new File("../com.google.cloud.tools.eclipse.appengine.newproject.maven/plugin.xml"));
    Assert.assertEquals("plugin", document.getDocumentElement().getNodeName());
  }
  
  @Test
  public void testValidExtensionPoints() throws Exception {
    Document document =
        parse(new File("../com.google.cloud.tools.eclipse.appengine.newproject.maven/plugin.xml"));
    NodeList extensions = document.getDocumentElement().getElementsByTagName("extension");
    for (int i = 0; i < extensions.getLength(); i++) {
      Element extension = (Element) extensions.item(i);
      String point = extension.getAttribute("point");
      IExtensionRegistry registry = RegistryFactory.getRegistry();
      IExtensionPoint extensionPoint = registry.getExtensionPoint(point);
      Assert.assertNotNull(extensionPoint);
    }
  }

  @Test
  public void testValidMavenLifecycleMapping() throws Exception {
    parse(new File(
        "../com.google.cloud.tools.eclipse.appengine.newproject.maven/lifecycle-mapping-metadata.xml"));
  }

}