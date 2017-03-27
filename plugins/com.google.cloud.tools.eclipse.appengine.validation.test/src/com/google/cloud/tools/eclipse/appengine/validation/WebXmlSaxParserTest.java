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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.xml.sax.SAXException;

public class WebXmlSaxParserTest {
  
  @Test
  public void testReadXml_emptyXml()
      throws ParserConfigurationException, IOException, SAXException {
    String emptyXml = "";
    byte[] bytes = emptyXml.getBytes(StandardCharsets.UTF_8);
    assert(WebXmlSaxParser.readXml(null, bytes).getBlacklist().isEmpty());
  }
  
  @Test
  public void testReadXml_xmlWithWrongVersion()
      throws ParserConfigurationException, IOException, SAXException {
    String xml = "<web-app xmlns='http://xmlns.jcp.org/xml/ns/javaee'"
        + " version='3.1'></web-app>";
    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    Queue<BannedElement> blacklist = WebXmlSaxParser.readXml(null, bytes).getBlacklist();
    String message = "App Engine Standard does not support this servlet version";
    assertEquals(blacklist.poll().getMessage(), message);
  }

}
