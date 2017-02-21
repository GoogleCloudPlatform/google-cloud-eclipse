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

public class BlacklistSaxParserTest {

  private static final String BANNED_ELEMENT = "<application></application>";
  private static final String XML_WITHOUT_BANNED_ELEMENT = "";
  private static final String XML_WITH_BANNED_ELEMENT = BANNED_ELEMENT;
  private static final String EMPTY_XML = "";
  private static final String BANNED_ELEMENT_MESSAGE =
      "Project ID should be specified at deploy time";
  
  @Test
  public void testReadXml_emptyXml()
      throws ParserConfigurationException, IOException, SAXException {
    byte[] bytes = EMPTY_XML.getBytes(StandardCharsets.UTF_8);
    assert(BlacklistSaxParser.readXml(bytes).getBlacklist().isEmpty());
  }
  
  @Test
  public void testReadXml_properXml()
      throws ParserConfigurationException, IOException, SAXException {
    byte[] bytes = XML_WITHOUT_BANNED_ELEMENT.getBytes(StandardCharsets.UTF_8);
    assert(BlacklistSaxParser.readXml(bytes).getBlacklist().isEmpty());
  }
  
  @Test
  public void testReadXml_xmlWithBannedElement()
      throws ParserConfigurationException, IOException, SAXException {
    byte[] bytes = XML_WITH_BANNED_ELEMENT.getBytes(StandardCharsets.UTF_8);
    Queue<BannedElement> blacklist = BlacklistSaxParser.readXml(bytes).getBlacklist();
    assertEquals(blacklist.poll().getMessage(), BANNED_ELEMENT_MESSAGE);
  }

}
