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
  
  // TODO we need to move these into a standard location
  private static final String RUNTIME_MARKER_ID =
      "com.google.cloud.tools.eclipse.appengine.validation.runtimeMarker";

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
 
    Element application =
        document.createElementNS("http://appengine.google.com/ns/1.0", "application");
    application.setUserData("location", new DocumentLocation(0, 25), null);
    root.appendChild(application);
    Element runtime =
        document.createElementNS("http://appengine.google.com/ns/1.0", "runtime");
    root.appendChild(runtime);
    
    List<ElementProblem> problems = validator.checkForProblems(null, document);
    assertEquals(1, problems.size());
    String markerId = "com.google.cloud.tools.eclipse.appengine.validation.applicationMarker";
    ElementProblem problem = problems.get(0);
    assertEquals(markerId, problem.getMarkerId());
    
    assertEquals(0, problem.getStart().getLineNumber());
    assertEquals(12, problem.getStart().getColumnNumber());
    assertEquals(27, problem.getLength());    
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
    ElementProblem problem = problems.get(0);
    assertEquals(RUNTIME_MARKER_ID, problem.getMarkerId());
    
    assertEquals(2, problem.getStart().getLineNumber());
    assertEquals(0, problem.getStart().getColumnNumber());
    assertEquals(19, problem.getLength());
  }  
  
  @Test
  public void testCheckForJava7() {
    Element runtime =
        document.createElementNS("http://appengine.google.com/ns/1.0", "runtime");
    runtime.setUserData("location", new DocumentLocation(0, 25), null);
    document.appendChild(runtime);
    Node java7 = document.createTextNode("java7");
    runtime.appendChild(java7);
    
    List<ElementProblem> problems = validator.checkForProblems(null, document);
    assertEquals(1, problems.size());
    ElementProblem problem = problems.get(0);
    assertEquals(RUNTIME_MARKER_ID, problem.getMarkerId());
    
    assertEquals(0, problem.getStart().getLineNumber());
    assertEquals(16, problem.getStart().getColumnNumber());
    assertEquals(24, problem.getLength());    
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
    assertEquals(RUNTIME_MARKER_ID, problems.get(0).getMarkerId());
  }
  
}
