package com.google.cloud.tools.eclipse.appengine.ui;

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

public class XmlTest {
    
  private Document document;
	
  @Before
  public void setUp() throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    document = builder.parse(
        new File("../com.google.cloud.tools.eclipse.appengine.ui/plugin.xml"));
  }
    
  @Test
  public void testPluginXml() {
    Assert.assertEquals("plugin", document.getDocumentElement().getNodeName());
  }
  
  @Test
  public void testLoadExtensionPoints() {
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