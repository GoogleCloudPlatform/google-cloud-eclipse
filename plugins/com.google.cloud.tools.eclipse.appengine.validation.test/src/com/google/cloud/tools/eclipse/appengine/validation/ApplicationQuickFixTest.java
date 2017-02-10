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
  
  private static final String APPLICATION_XML =
      "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
      + "<application>"
      + "</application>"
      + "</appengine-web-app>";
  
  private static final String STYLESHEET = "/xslt/application.xsl";
  
  @Test
  public void testApplyXslt()
      throws IOException, ParserConfigurationException, SAXException, TransformerException {
    try (InputStream xmlStream = stringToInputStream(APPLICATION_XML);
        InputStream stylesheetStream = ApplicationQuickFix.class.getResourceAsStream(STYLESHEET)) {
      
      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      builderFactory.setNamespaceAware(true);
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
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
