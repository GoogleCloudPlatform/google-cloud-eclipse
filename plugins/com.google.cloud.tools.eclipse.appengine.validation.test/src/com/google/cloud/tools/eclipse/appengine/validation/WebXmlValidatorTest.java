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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.util.ArrayList;
import java.util.function.BiPredicate;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@RunWith(MockitoJUnitRunner.class)
public class WebXmlValidatorTest {

  @Rule
  public TestProjectCreator projectCreator = new TestProjectCreator().withFacets(
      JavaFacet.VERSION_1_7);

  @Mock private BiPredicate<IProject, String> servletApiSupportChecker;
  @Mock private IResource resource;

  private WebXmlValidator validator;

  @Before
  public void setUp() {
    validator = new WebXmlValidator(servletApiSupportChecker);
    
    IProject project = projectCreator.getProject();
    when(resource.getProject()).thenReturn(project);

    // Assume the project has the Dynamic Web Module Facet 2.5.
    when(servletApiSupportChecker.test(eq(project), anyString())).thenReturn(false);
    when(servletApiSupportChecker.test(project, "2.5")).thenReturn(true);
  }

  @Test
  public void testValidateJavaServlet() throws ParserConfigurationException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
    Document document = documentBuilder.newDocument();

    Element element = document.createElementNS("http://xmlns.jcp.org/xml/ns/javaee", "web-app");
    element.setUserData("version", "3.1", null);
    element.setUserData("location", new DocumentLocation(1, 1), null);
    document.appendChild(element);

    ArrayList<ElementProblem> problems = validator.checkForProblems(resource, document);

    assertEquals(1, problems.size());
    String markerId = "com.google.cloud.tools.eclipse.appengine.validation.servletMarker";
    assertEquals(markerId, problems.get(0).getMarkerId());
  }

  @Test
  public void testCheckForElements_noElements() throws ParserConfigurationException {

    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
    Document document = documentBuilder.newDocument();

    Element element = document.createElementNS("http://java.sun.com/xml/ns/javaee", "web-app");
    element.setUserData("version", "2.5", null);
    element.setUserData("location", new DocumentLocation(1, 1), null);
    document.appendChild(element);

    ArrayList<ElementProblem> problems = validator.checkForProblems(resource, document);
    assertEquals(0, problems.size());
  }

  @Test
  public void testValidateServletMapping() throws ParserConfigurationException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
    Document document = documentBuilder.newDocument();

    Element webApp = document.createElementNS("http://java.sun.com/xml/ns/javaee", "web-app");
    webApp.setUserData("version", "2.5", null);
    webApp.setUserData("location", new DocumentLocation(1, 1), null);

    Element servlet = document.createElementNS("http://java.sun.com/xml/ns/javaee", "servlet");
    servlet.setUserData("location", new DocumentLocation(2, 1), null);

    Element servletName = document.createElementNS("http://java.sun.com/xml/ns/javaee", "servlet-name");
    servletName.setTextContent("ServletName");
    servletName.setUserData("location", new DocumentLocation(3, 1), null);
    servlet.appendChild(servletName);
    webApp.appendChild(servlet);

    Element servletMapping = document.createElementNS("http://java.sun.com/xml/ns/javaee", "servlet-mapping");
    servletMapping.setUserData("location", new DocumentLocation(4, 1), null);

    Element servletMappingName = document.createElementNS("http://java.sun.com/xml/ns/javaee", "servlet-name");
    servletMappingName.setTextContent("NotServletName");
    servletMappingName.setUserData("location", new DocumentLocation(2, 1), null);
    servletMapping.appendChild(servletMappingName);
    webApp.appendChild(servletMapping);

    document.appendChild(webApp);

    ArrayList<ElementProblem> problems = validator.checkForProblems(resource, document);
    assertEquals(1, problems.size());
  }

}
