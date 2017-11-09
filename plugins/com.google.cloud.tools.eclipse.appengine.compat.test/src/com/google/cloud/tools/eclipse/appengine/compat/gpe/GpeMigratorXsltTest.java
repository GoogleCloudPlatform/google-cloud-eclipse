/*
 * Copyright 2017 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.compat.gpe;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.cloud.tools.eclipse.util.Xslt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GpeMigratorXsltTest {

  private static final String WTP_METADATA_XML = "<?xml version='1.0' encoding='UTF-8'?>"
      + "<faceted-project>"
      + "  <runtime name='Google App Engine'/>"
      + "  <runtime name='App Engine Standard Runtime'/>"
      + "  <installed facet='java' version='1.7'/>"
      + "  <installed facet='jst.web' version='2.5'/>"
      + "  <installed facet='com.google.appengine.facet' version='1'/>"
      + "  <installed facet='com.google.appengine.facet.ear' version='1'/>"
      + "</faceted-project>";

  private static final String STYLESHEET = "<?xml version='1.0' encoding='UTF-8'?>"
      + "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
      + "  <xsl:template match='faceted-project'>"
      + "    <xsl:copy/>"
      + "  </xsl:template>"
      + "</xsl:stylesheet>";

  private static final Path wtpMetadataXslPath =
      Paths.get("../com.google.cloud.tools.eclipse.appengine.compat/xslt/wtpMetadata.xsl");

  private static final DocumentBuilderFactory documentBuilderFactory
      = DocumentBuilderFactory.newInstance();

  @BeforeClass
  public static void setUp() {
    documentBuilderFactory.setNamespaceAware(true);
  }

  @Test
  public void testApplyXslt()
      throws IOException, ParserConfigurationException, SAXException, TransformerException {
    try (InputStream xmlStream = stringToInputStream(WTP_METADATA_XML);
        InputStream stylesheetStream = stringToInputStream(STYLESHEET);
        InputStream inputStream = Xslt.applyXslt(xmlStream, stylesheetStream)) {
      DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
      Document transformed = builder.parse(inputStream);
      assertEquals(0, transformed.getDocumentElement().getChildNodes().getLength());
    }
  }

  @Test
  public void testWtpMetadataStylesheet_removesGpeRuntimeAndFacets()
      throws IOException, ParserConfigurationException, SAXException, TransformerException {
    try (InputStream xmlStream = stringToInputStream(WTP_METADATA_XML);
        InputStream stylesheetStream = Files.newInputStream(wtpMetadataXslPath);
        InputStream inputStream = Xslt.applyXslt(xmlStream, stylesheetStream)) {
      DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
      Document transformed = builder.parse(inputStream);

      assertArrayEquals(new String[]{"App Engine Standard Runtime"},
          getAttributesByTagNameAndAttributeName(transformed, "runtime", "name"));
      assertArrayEquals(new String[]{"java", "jst.web"},
          getAttributesByTagNameAndAttributeName(transformed, "installed", "facet"));
    }
  }

  private static InputStream stringToInputStream(String string) {
    return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
  }

  private static String[] getAttributesByTagNameAndAttributeName(
      Document document, String tagName, String attributeName) {
    NodeList nodes = document.getElementsByTagName(tagName);
    String[] attributes = new String[nodes.getLength()];

    for (int i = 0; i < nodes.getLength(); i++) {
      attributes[i] = ((Element) nodes.item(i)).getAttribute(attributeName);
    }
    return attributes;
  }
}
