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

import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class PomXmlValidatorTest {

  @Test
  public void testCheckForElements() throws ParserConfigurationException {

    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
    Document document = documentBuilder.newDocument();

    Element groupId = document.createElementNS("http://maven.apache.org/POM/4.0.0", "groupId");
    groupId.setUserData("location", new DocumentLocation(2, 1), null);
    groupId.setTextContent("com.google.appengine");
    
    Element artifactId = document.createElementNS("http://maven.apache.org/POM/4.0.0", "artifactId");
    artifactId.setUserData("location", new DocumentLocation(3, 1), null);
    artifactId.setTextContent("appengine-maven-plugin");
    
    Element plugin = document.createElementNS("http://maven.apache.org/POM/4.0.0", "plugin");
    plugin.setUserData("location", new DocumentLocation(4, 1), null);
    plugin.appendChild(groupId);
    plugin.appendChild(artifactId);
    
    Element plugins = document.createElementNS("http://maven.apache.org/POM/4.0.0", "plugins");
    plugins.setUserData("location", new DocumentLocation(5, 1), null);
    plugins.appendChild(plugin);
    
    Element build = document.createElementNS("http://maven.apache.org/POM/4.0.0", "build");
    build.setUserData("location", new DocumentLocation(6, 1), null);
    build.appendChild(plugins);

    document.appendChild(build);
  
    PomXmlValidator validator = new PomXmlValidator();
    ArrayList<BannedElement> blacklist = validator.checkForElements(null, document);
    assertEquals(1, blacklist.size());
    String markerId = "com.google.cloud.tools.eclipse.appengine.validation.mavenPluginMarker";
    assertEquals(markerId, blacklist.get(0).getMarkerId());
  }
  
  @Test
  public void testCheckForElements_noElements() throws ParserConfigurationException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
    Document document = documentBuilder.newDocument();
    
    Element plugin = document.createElementNS("http://maven.apache.org/POM/4.0.0", "plugin");
    plugin.setUserData("location", new DocumentLocation(1, 1), null);
    
    Element groupId = document.createElementNS("http://maven.apache.org/POM/4.0.0", "groupId");
    groupId.setUserData("location", new DocumentLocation(2, 1), null);
    groupId.setTextContent("com.google.cloud.tools");
    plugin.appendChild(groupId);
    
    Element artifactId = document.createElementNS("http://maven.apache.org/POM/4.0.0", "artifactId");
    artifactId.setUserData("location", new DocumentLocation(3, 1), null);
    artifactId.setTextContent("appengine-maven-plugin");
    plugin.appendChild(artifactId);

    document.appendChild(plugin);
    
    PomXmlValidator validator = new PomXmlValidator();
    ArrayList<BannedElement> blacklist = validator.checkForElements(null, document);
    
    assertEquals(0, blacklist.size());
  }
  
  @Test
  public void testCheckForElements_multiplePluginTags() throws ParserConfigurationException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
    Document document = documentBuilder.newDocument();
    Element rootPlugin = document.createElementNS("http://maven.apache.org/POM/4.0.0", "plugins");
    
    //plugin #1
    Element markedPlugin = document.createElementNS("http://maven.apache.org/POM/4.0.0", "plugin");
    markedPlugin.setUserData("location", new DocumentLocation(1, 1), null);
    
    Element groupId1 = document.createElementNS("http://maven.apache.org/POM/4.0.0", "groupId");
    groupId1.setUserData("location", new DocumentLocation(2, 1), null);
    groupId1.setTextContent("com.google.appengine");
    markedPlugin.appendChild(groupId1);
    
    Element artifactId1 = document.createElementNS("http://maven.apache.org/POM/4.0.0", "artifactId");
    artifactId1.setUserData("location", new DocumentLocation(3, 1), null);
    artifactId1.setTextContent("appengine-maven-plugin");
    markedPlugin.appendChild(artifactId1);

    rootPlugin.appendChild(markedPlugin);
    
    //plugin #2
    Element ignoredPlugin = document.createElementNS("http://maven.apache.org/POM/4.0.0", "plugin");
    ignoredPlugin.setUserData("location", new DocumentLocation(4, 1), null);
    
    Element groupId2 = document.createElementNS("http://maven.apache.org/POM/4.0.0", "groupId");
    groupId2.setUserData("location", new DocumentLocation(5, 1), null);
    groupId2.setTextContent("com.google.cloud.tools");
    ignoredPlugin.appendChild(groupId2);
    
    Element artifactId2 = document.createElementNS("http://maven.apache.org/POM/4.0.0", "artifactId");
    artifactId2.setUserData("location", new DocumentLocation(6, 1), null);
    artifactId2.setTextContent("appengine-maven-plugin");
    ignoredPlugin.appendChild(artifactId2);

    rootPlugin.appendChild(ignoredPlugin);
    
    //plugin #3
    Element ignoredPlugin2 = document.createElementNS("http://maven.apache.org/POM/4.0.0", "plugin");
    markedPlugin.setUserData("location", new DocumentLocation(7, 1), null);
    
    Element groupId3 = document.createElementNS("http://maven.apache.org/POM/4.0.0", "groupId");
    groupId3.setUserData("location", new DocumentLocation(8, 1), null);
    groupId3.setTextContent("com.google.appengine");
    ignoredPlugin2.appendChild(groupId3);
    
    Element artifactId3 = document.createElementNS("http://maven.apache.org/POM/4.0.0", "artifactId");
    artifactId3.setUserData("location", new DocumentLocation(9, 1), null);
    artifactId3.setTextContent("ignore this case");
    ignoredPlugin2.appendChild(artifactId3);
    rootPlugin.appendChild(ignoredPlugin2);
    
    document.appendChild(rootPlugin);
    
    PomXmlValidator validator = new PomXmlValidator();
    ArrayList<BannedElement> blacklist = validator.checkForElements(null, document);
    
    assertEquals(1, blacklist.size());
    String markerId = "com.google.cloud.tools.eclipse.appengine.validation.mavenPluginMarker";
    assertEquals(markerId, blacklist.get(0).getMarkerId());
  }
}
