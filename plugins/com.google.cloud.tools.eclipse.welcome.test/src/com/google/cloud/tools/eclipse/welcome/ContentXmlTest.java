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

package com.google.cloud.tools.eclipse.welcome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ContentXmlTest {

  @Test
  public void testWellFormed() throws ParserConfigurationException, IOException, SAXException {
    Document document = parseDocument("intro/cloud-tools-for-eclipse.xml");
    assertEquals(6, document.getElementsByTagName("extensionContent").getLength());
  }

  @Test
  public void testContributionsHaveId()
      throws ParserConfigurationException, IOException, SAXException {
    Document document = parseDocument("intro/cloud-tools-for-eclipse.xml");
    NodeList contributions = document.getElementsByTagName("extensionContent");
    for (int i = 0 ; i < contributions.getLength(); i++) {
      Element contribution = (Element) contributions.item(i);
      assertEquals("extensionContent", contribution.getNodeName());
      assertFalse(Strings.isNullOrEmpty(contribution.getAttribute("id")));
      assertFalse(Strings.isNullOrEmpty(contribution.getAttribute("name")));
      assertFalse(Strings.isNullOrEmpty(contribution.getAttribute("path")));
      assertFalse(Strings.isNullOrEmpty(contribution.getAttribute("style")));
    }
  }

  /** Parse the XML document within the plugin-under-test. */
  private Document parseDocument(String filePath)
      throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();

    try (InputStream contentXml =
        Files.newInputStream(Paths.get("../com.google.cloud.tools.eclipse.welcome/" + filePath))) {
      Document document = builder.parse(contentXml);
      return document;
    }
  }
}
