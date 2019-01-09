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

import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class AppEngineWebXmlValidatorTest {
  
  private Document document;
  private final AppEngineWebXmlValidator validator = new AppEngineWebXmlValidator();

  @Before
  public void setUp() throws ParserConfigurationException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder  = builderFactory.newDocumentBuilder();
    
    document = documentBuilder.newDocument();
  }
  
  @Test
  public void testCheckForApplication() {
    Element root =
        document.createElementNS("http://appengine.google.com/ns/1.0", "appengine-web-app");
    document.appendChild(root);
 
    Element element =
        document.createElementNS("http://appengine.google.com/ns/1.0", "application");
    element.setUserData("location", new DocumentLocation(3, 15), null);
    root.appendChild(element);
    Element runtime =
        document.createElementNS("http://appengine.google.com/ns/1.0", "runtime");
    root.appendChild(runtime);
    
    List<ElementProblem> blacklist = validator.checkForProblems(null, document);
    assertEquals(1, blacklist.size());
    String markerId = "com.google.cloud.tools.eclipse.appengine.validation.applicationMarker";
    assertEquals(markerId, blacklist.get(0).getMarkerId());
  }

  @Test
  public void testCheckForNoRuntime() {
    Element element =
        document.createElementNS("http://appengine.google.com/ns/1.0", "appengine-web-app");
    document.appendChild(element);
    
    DocumentLocation location = new DocumentLocation(2, 1);
    document.getDocumentElement().setUserData("location", location, null);
    
    List<ElementProblem> problems = validator.checkForProblems(null , document);
    assertEquals(1, problems.size());
    String markerId = "com.google.cloud.tools.eclipse.appengine.validation.runtimeMarker";
    assertEquals(markerId, problems.get(0).getMarkerId());
  }  
  
  @Test
  public void testCheckForJava7() {
    Element runtime =
        document.createElementNS("http://appengine.google.com/ns/1.0", "runtime");
    runtime.setUserData("location", new DocumentLocation(3, 15), null);
    document.appendChild(runtime);
    Node java7 = document.createTextNode("java7");
    runtime.appendChild(java7);
    
    List<ElementProblem> problems = validator.checkForProblems(null, document);
    assertEquals(1, problems.size());
    String markerId = "com.google.cloud.tools.eclipse.appengine.validation.runtimeMarker";
    assertEquals(markerId, problems.get(0).getMarkerId());
  }  

  @Test
  public void testCheckForJava6() {
    Element element =
        document.createElementNS("http://appengine.google.com/ns/1.0", "runtime");
    element.setUserData("location", new DocumentLocation(3, 15), null);
    document.appendChild(element);
    Node java6 = document.createTextNode("java"); // sic; java, not java6
    element.appendChild(java6);
    
    List<ElementProblem> problems = validator.checkForProblems(null, document);
    assertEquals(1, problems.size());
    String markerId = "com.google.cloud.tools.eclipse.appengine.validation.runtimeMarker";
    assertEquals(markerId, problems.get(0).getMarkerId());
  }
  
}
