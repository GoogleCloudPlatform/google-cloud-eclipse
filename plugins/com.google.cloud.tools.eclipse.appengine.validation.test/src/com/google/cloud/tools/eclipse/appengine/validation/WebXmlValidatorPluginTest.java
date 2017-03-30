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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.common.collect.Lists;

public class WebXmlValidatorPluginTest {
 
  private IJavaProject javaProject;
  private IResource resource;
  
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator()
      .withFacetVersions(Lists.newArrayList(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25));
  
  @Before
  public void setUp() throws CoreException {
    // Project's default source folder is the main project folder.
    IProject project = projectCreator.getProject();
    javaProject = projectCreator.getJavaProject();
    
    resource = project.getFile("WebContent/WEB-INF/web.xml");
    
    ValidationTestUtils.createFolders(project, new Path("src/main/java"));
    IFile servletClass = project.getFile("src/main/java/ServletClass.java");
    servletClass.create(
        new ByteArrayInputStream("public class ServletClass {}".getBytes(StandardCharsets.UTF_8)),
        true, null);
    
    ValidationTestUtils.createFolders(project, new Path("src/com/example"));
    IFile servletClassInPackage = project.getFile("src/com/example/ServletClassInPackage.java");
    servletClassInPackage.create(
        new ByteArrayInputStream("package com.example; public class ServletClassInPackage {}"
            .getBytes(StandardCharsets.UTF_8)), true, null);
  }
  
  @Test
  public void testCheckForElements_servletClass() throws ParserConfigurationException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
    Document document = documentBuilder.newDocument();
    
    Element element = document.createElement("servlet-class");
    element.setTextContent("DoesNotExist");
    element.setUserData("location", new DocumentLocation(1, 1), null);
    document.appendChild(element);
    
    WebXmlValidator validator = new WebXmlValidator();
    ArrayList<BannedElement> blacklist = validator.checkForElements(resource, document);
    
    assertEquals(1, blacklist.size());
    String markerId = "com.google.cloud.tools.eclipse.appengine.validation.undefinedServletMarker";
    assertEquals(markerId, blacklist.get(0).getMarkerId());
  }
  
  @Test
  public void testCheckForElements_servletClassExists() throws ParserConfigurationException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
    Document document = documentBuilder.newDocument();
    
    Element element = document.createElement("servlet-class");
    element.setTextContent("ServletClass");
    element.setUserData("location", new DocumentLocation(1, 1), null);
    document.appendChild(element);
    
    WebXmlValidator validator = new WebXmlValidator();
    ArrayList<BannedElement> blacklist = validator.checkForElements(resource, document);
    
    assertEquals(0, blacklist.size());
  }
  
  @Test
  public void testClassExists() {
    assertFalse(WebXmlValidator.classExists(javaProject, "DoesNotExist"));
    assertFalse(WebXmlValidator.classExists(null, null));
    assertFalse(WebXmlValidator.classExists(null, ""));
    assertTrue(WebXmlValidator.classExists(javaProject, "ServletClass"));
  }
  
  @Test
  public void testClassExists_inPackage() {
    assertTrue(WebXmlValidator.classExists(javaProject, "com.example.ServletClassInPackage"));
  }
 
}
