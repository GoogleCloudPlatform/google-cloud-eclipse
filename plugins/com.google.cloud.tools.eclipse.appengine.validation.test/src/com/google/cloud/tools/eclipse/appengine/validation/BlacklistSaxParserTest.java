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
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.xml.sax.SAXException;

public class BlacklistSaxParserTest {

  private static final String TEST_ID = "fooId";
  private static final String VERSION =
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
  private static final String ROOT_END_TAG = "</appengine-web-app>";
  private static final String ROOT_START_TAG =
      "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>";
  private static final String BANNED_ELEMENT = "<application>" + TEST_ID + "</application>";
  private static final String XML_WITHOUT_BANNED_ELEMENT =
      VERSION + ROOT_START_TAG + ROOT_END_TAG;
  private static final String XML_WITH_BANNED_ELEMENT = 
      VERSION + ROOT_START_TAG + BANNED_ELEMENT + ROOT_END_TAG;
  private static final String EMPTY_XML = "";
  private static final String BANNED_ELEMENT_MESSAGE = "project ID tag not recommended";
  
  @Test
  public void testReadXml_emptyXml()
      throws ParserConfigurationException, IOException, SAXException {
    byte[] bytes = EMPTY_XML.getBytes();
    assert(BlacklistSaxParser.readXml(bytes).isEmpty());
  }
  
  @Test
  public void testReadXml_properXml()
      throws ParserConfigurationException, IOException, SAXException {
    byte[] bytes = XML_WITHOUT_BANNED_ELEMENT.getBytes();
    assert(BlacklistSaxParser.readXml(bytes).isEmpty());
  }
  
  @Test
  public void testReadXml_xmlWithBannedElement()
      throws ParserConfigurationException, IOException, SAXException {
    byte[] bytes = XML_WITH_BANNED_ELEMENT.getBytes();
    Stack<BannedElement> blacklist = BlacklistSaxParser.readXml(bytes);
    assertEquals(blacklist.pop().getMessage(), BANNED_ELEMENT_MESSAGE);
  }

}
