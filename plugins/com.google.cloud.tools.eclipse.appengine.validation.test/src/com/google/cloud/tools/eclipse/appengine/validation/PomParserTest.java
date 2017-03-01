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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.xml.sax.SAXException;

public class PomParserTest {
  
  private static final String PROJECT_START_TAG = "<project xmlns='http://maven.apache.org/POM/4.0.0' "
      + "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
      + "xsi:schemaLocation='http://maven.apache.org/POM/4.0.0 "
      + "http://maven.apache.org/xsd/maven-4.0.0.xsd'>";
  
  @Test
  public void testReadXml_emptyXml()
      throws ParserConfigurationException, IOException, SAXException {
    String emptyXml = "";
    byte[] bytes = emptyXml.getBytes(StandardCharsets.UTF_8);
    assert(BlacklistSaxParser.readXml(bytes).getBlacklist().isEmpty());
  }
  
  @Test
  public void testReadXml_xmlWithBannedElement()
      throws ParserConfigurationException, IOException, SAXException {
    String xml = PROJECT_START_TAG
        + "<build><plugins><plugin><groupId>com.google.appengine</groupId>"
        + "<artifactId>appengine-maven-plugin</artifactId></plugin></plugins></build></project>";
    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    Queue<BannedElement> blacklist = PomParser.readXml(bytes).getBlacklist();
    String message = Messages.getString("maven.plugin");
    assertEquals(blacklist.poll().getMessage(), message);
  }
  
  @Test
  public void testReadXml_differentOrder()
      throws ParserConfigurationException, IOException, SAXException {
    String xml = PROJECT_START_TAG
        + "<build><plugins><plugin><artifactId>appengine-maven-plugin</artifactId>"
        + "<groupId>com.google.appengine</groupId></plugin></plugins></build></project>";
    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    Queue<BannedElement> blacklist = PomParser.readXml(bytes).getBlacklist();
    String message = Messages.getString("maven.plugin");
    assertEquals(blacklist.poll().getMessage(), message);
  }
  
  @Test
  public void testReadXml_noBannedElements()
      throws ParserConfigurationException, IOException, SAXException {
    String xml = PROJECT_START_TAG
        + "<build><plugins><plugin><groupId>com.google.tools</groupId>"
        + "<artifactId>appengine-maven-plugin</artifactId></plugin></plugins></build></project>";
    byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
    Queue<BannedElement> blacklist = PomParser.readXml(bytes).getBlacklist();
    assertTrue(blacklist.isEmpty());
  }

}
