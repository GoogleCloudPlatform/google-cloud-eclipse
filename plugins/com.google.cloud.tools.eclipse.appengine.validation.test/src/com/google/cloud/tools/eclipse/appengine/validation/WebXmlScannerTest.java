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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Locator2Impl;
import org.xml.sax.helpers.AttributesImpl;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.common.collect.Lists;

public class WebXmlScannerTest {

  private static IFile resource;
  private static IJavaProject javaProject;
  private WebXmlScanner scanner;
  
  @ClassRule public static TestProjectCreator projectCreator = new TestProjectCreator()
      .withFacetVersions(Lists.newArrayList(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25));
  
  @BeforeClass
  public static void setUpBeforeClass() throws CoreException {
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
  
  @Before
  public void setUp() throws SAXException {
    scanner = new WebXmlScanner(resource);
    scanner.setDocumentLocator(new Locator2Impl());
    scanner.startDocument();
  }
  
  @Test
  public void testCaseSensitive() throws SAXException {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", "", "version", "", "3.1");
    scanner.startElement("http://xmlns.jcp.org/xml/ns/javaee", "Web-App", "", attributes);
    assertTrue(scanner.getBlacklist().isEmpty());
  }
  
  @Test
  public void testStartElement_jcpNamespace() throws SAXException {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", "", "version", "", "3.1");
    scanner.startElement("http://xmlns.jcp.org/xml/ns/javaee", "web-app", "", attributes);
    assertEquals(1, scanner.getBlacklist().size());
    String message = scanner.getBlacklist().peek().getMessage();
    String expectedMessage = "App Engine Standard does not support this servlet version";
    assertEquals(expectedMessage, message);
  }
  
  @Test
  public void testStartElement_sunNamespace() throws SAXException {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", "", "version", "", "3.0");
    scanner.startElement("http://java.sun.com/xml/ns/javaee", "web-app", "", attributes);
    assertEquals(1, scanner.getBlacklist().size());
    String message = scanner.getBlacklist().peek().getMessage();
    String expectedMessage = "App Engine Standard does not support this servlet version";
    assertEquals(expectedMessage, message);
  }
  
  @Test
  public void testStartElement_correctVersion() throws SAXException {
    AttributesImpl attributes = new AttributesImpl();
    attributes.addAttribute("", "", "version", "", "2.5");
    scanner.startElement("http://java.sun.com/xml/ns/javaee", "web-app", "", attributes);
    assertEquals(0, scanner.getBlacklist().size());
  }
  
  @Test
  public void testFindClass() {
    assertFalse(WebXmlScanner.classExists(javaProject, "DoesNotExist"));
    assertFalse(WebXmlScanner.classExists(null, null));
    assertFalse(WebXmlScanner.classExists(null, ""));
    assertTrue(WebXmlScanner.classExists(javaProject, "ServletClass"));
  }
  
  @Test
  public void testFindClass_inPackage() {
    assertTrue(WebXmlScanner.classExists(javaProject, "com.example.ServletClassInPackage"));
  }
  
}
