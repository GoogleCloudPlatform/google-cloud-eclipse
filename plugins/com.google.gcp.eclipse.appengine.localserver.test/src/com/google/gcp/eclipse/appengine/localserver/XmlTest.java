package com.google.gcp.eclipse.appengine.localserver;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class XmlTest {
    
  private DocumentBuilder builder;
	
  @Before
  public void setUp() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    builder = factory.newDocumentBuilder();
  }
	
  @Test
  public void testGcpXml() throws Exception {
    // test fails if malformed
    Document doc = builder.parse(
        new File("../com.google.gcp.eclipse.appengine.localserver/gcp.xml"));
    Assert.assertEquals("project", doc.getDocumentElement().getNodeName());
  }
    
  @Test
  public void testGcloudServerDef() throws Exception {
    // test fails if malformed
    Document doc = builder.parse(
        new File("../com.google.gcp.eclipse.appengine.localserver/gcloud_serverdef.xml"));
    Assert.assertEquals("ServerRuntime", doc.getDocumentElement().getLocalName());
  }
    
  @Test
  public void testPluginXml() throws Exception {
    // test fails if malformed
    Document doc = builder.parse(
        new File("../com.google.gcp.eclipse.appengine.localserver/plugin.xml"));
    Assert.assertEquals("plugin", doc.getDocumentElement().getNodeName());
  }
}