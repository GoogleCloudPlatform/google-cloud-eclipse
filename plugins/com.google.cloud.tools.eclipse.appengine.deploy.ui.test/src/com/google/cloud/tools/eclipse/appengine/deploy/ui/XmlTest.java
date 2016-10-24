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

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineFlexFacet;
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
    Assert.assertEquals(4, tests.getLength());
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
    Assert.assertEquals(2, adaptTestNodes.getLength());
    Element adaptTestEntry1 = (Element) adaptTestNodes.item(0);
    String adaptTestProperty = "org.eclipse.wst.common.project.facet.core.projectFacet";
    Assert.assertEquals(adaptTestProperty, adaptTestEntry1.getAttribute("property"));
    Assert.assertEquals(AppEngineStandardFacet.ID, adaptTestEntry1.getAttribute("value"));
    Element adaptTestEntry2 = (Element) adaptTestNodes.item(1);
    Assert.assertEquals(adaptTestProperty, adaptTestEntry2.getAttribute("property"));
    Assert.assertEquals(AppEngineFlexFacet.ID, adaptTestEntry2.getAttribute("value"));
  }
}
