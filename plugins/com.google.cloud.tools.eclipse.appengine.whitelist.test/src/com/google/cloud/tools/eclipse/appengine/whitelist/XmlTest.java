package com.google.cloud.tools.eclipse.appengine.whitelist;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class XmlTest {
    
  private Document document;
	
  @Before
  public void setUp() throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    document = builder.parse(
        new File("../com.google.cloud.tools.eclipse.appengine.whitelist/plugin.xml"));
  }
    
  @Test
  public void testPluginXml() {
    Assert.assertEquals("plugin", document.getDocumentElement().getNodeName());
  }
  
  @Test
  public void testLoadExtensionClass() {
    Element runElement = (Element) document
        .getDocumentElement()
        .getElementsByTagName("compilationParticipant")
        .item(0);
    String className = runElement.getAttribute("class");
    try {
      Class.forName(className).newInstance();
    } catch (ClassNotFoundException ex) {
      Assert.fail("Could not load class " + className + " referenced in plugin.xml");
    } catch (InstantiationException ex) {
      Assert.fail("Class " + className + " does not have a no-arg constructor");
    } catch (IllegalAccessException e) {
      Assert.fail("Class " + className + " no-arg constructor is not public");
    }
  }
  
}