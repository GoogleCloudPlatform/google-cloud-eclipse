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

package com.google.cloud.tools.eclipse.appengine.validation;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ApplicationQuickFixTest {
  
  private static final String APPLICATION_XML = "<?xml version='1.0' encoding='utf-8'?>"
      + "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
      + "<application>"
      + "</application>"
      + "</appengine-web-app>";
  
  private static final String STYLESHEET = "<?xml version='1.0' encoding='UTF-8'?>"
      + "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
      + "<xsl:template match='node()|@*'>"
      + "<xsl:copy>"
      + "<xsl:apply-templates select='node()|@*'/>"
      + "</xsl:copy>"
      + "</xsl:template>"
      + "<xsl:template match='application'/>"
      + "</xsl:stylesheet>";
  
  @Test
  public void testApplyXslt()
      throws IOException, ParserConfigurationException, SAXException, TransformerException {
    try (InputStream xmlStream = stringToInputStream(APPLICATION_XML);
        InputStream stylesheetStream = stringToInputStream(STYLESHEET)) {

      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = builder.parse(xmlStream);

      try (InputStream inputStream = ApplicationQuickFix.applyXslt(document, stylesheetStream)) {
        Document transformed = builder.parse(inputStream);

        assertEquals(1, document.getDocumentElement().getChildNodes().getLength());
        assertEquals(0, transformed.getDocumentElement().getChildNodes().getLength());
      }
    }
  }
  
  private static InputStream stringToInputStream(String string) {
    return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
  }

}
