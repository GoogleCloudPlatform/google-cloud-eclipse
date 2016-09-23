package com.google.cloud.tools.eclipse.appengine.deploy.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.either;
import static org.junit.Assert.assertThat;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;

public class XmlTest {

  private Document doc;

  @Before
  public void setUp() throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    // test fails if malformed
    doc = builder.parse(
        new File("../com.google.cloud.tools.eclipse.appengine.deploy.ui/plugin.xml"));
  }

  @Test
  public void testLimitedVisibility() {
    NodeList pages = doc.getElementsByTagName("page");
    Assert.assertEquals(2, pages.getLength());
    NodeList enabledWhen = doc.getElementsByTagName("enabledWhen");
    Assert.assertEquals(3, enabledWhen.getLength());
    NodeList tests = doc.getElementsByTagName("test");
    Assert.assertEquals(3, tests.getLength());
    NodeList adapts = doc.getElementsByTagName("adapt");
    Assert.assertEquals(1, adapts.getLength());

    for (int i = 0; i < enabledWhen.getLength(); i++) {
      Element element = (Element) enabledWhen.item(i);
      Node parent = element.getParentNode();
      assertThat(parent.getNodeName(), either(is("page")).or(is("handler")));
    }

    Element adapt = (Element) adapts.item(0);
    Assert.assertEquals("org.eclipse.core.resources.IProject", adapt.getAttribute("type"));

    NodeList adaptTestNodes = adapt.getElementsByTagName("test");
    Assert.assertEquals(1, adaptTestNodes.getLength());
    Element adaptTestEntry = (Element) adaptTestNodes.item(0);
    Assert.assertEquals("org.eclipse.wst.common.project.facet.core.projectFacet", adaptTestEntry.getAttribute("property"));
    Assert.assertEquals(AppEngineStandardFacet.ID, adaptTestEntry.getAttribute("value"));
  }
}