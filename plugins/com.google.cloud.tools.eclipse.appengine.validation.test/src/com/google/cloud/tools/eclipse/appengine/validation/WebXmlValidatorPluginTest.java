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

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WebXmlValidatorPluginTest {

  private IJavaProject javaProject;
  private IResource resource;
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator().withFacetVersions(
      JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25, AppEngineStandardFacet.FACET_VERSION);

  @Before
  public void setUp() throws CoreException {
    IProject project = projectCreator.getProject();
    javaProject = projectCreator.getJavaProject();

    resource = project.getFile("WebContent/WEB-INF/web.xml");

    createFile(project, "src/main/java", "ServletClass.java", "public class ServletClass {}");
    createFile(project, "src/com/example", "ServletClassInPackage.java",
        "package com.example; public class ServletClassInPackage {}");
    createFile(project, "WebContent", "InWebContent.jsp", "");
    createFile(project, "src", "InSrc.jsp", "");
  }

  @After
  public void tearDown() throws CoreException {
    // Delete to avoid https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1916
    javaProject.getProject().getFile("WebContent/InWebContent.jsp").delete(true, null);
    javaProject.getProject().getFile("src/InSrc.jsp").delete(true, null);
  }

  private static void createFile(IProject project, String folder, String filename,
      String contents) throws CoreException {
    ResourceUtils.createFolders(project.getFolder(folder), null);
    IFile file = project.getFile(folder + "/" + filename);
    file.create(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)), true, null);
  }

  @Test
  public void testCheckForElements_servletClass() throws ParserConfigurationException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
    Document document = documentBuilder.newDocument();

    Element root = document.createElement("web-app");
    root.setUserData("version", "2.5", null);
    root.setUserData("location", new DocumentLocation(1, 1), null);

    Element element = document.createElement("servlet-class");
    element.setTextContent("DoesNotExist");
    element.setUserData("location", new DocumentLocation(2, 1), null);
    root.appendChild(element);
    document.appendChild(root);

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

    Element root = document.createElement("web-app");
    root.setUserData("version", "2.5", null);
    root.setUserData("location", new DocumentLocation(1, 1), null);

    Element element = document.createElement("servlet-class");
    element.setTextContent("ServletClass");
    element.setUserData("location", new DocumentLocation(2, 1), null);
    root.appendChild(element);
    document.appendChild(root);

    WebXmlValidator validator = new WebXmlValidator();
    ArrayList<BannedElement> blacklist = validator.checkForElements(resource, document);

    assertTrue(blacklist.isEmpty());
  }

  @Test
  public void testValidateJsp() throws ParserConfigurationException {
    // For a typical dynamic web project:
    //     /           -> WebContent
    // WEB-INF         -> WebContent/WEB-INF
    // WEB-INF/classes -> src

    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
    Document document = documentBuilder.newDocument();

    Element root = document.createElement("web-app");
    root.setUserData("location", new DocumentLocation(1, 1), null);
    root.setUserData("version", "2.5", null);

    Element element = document.createElement("jsp-file");
    element.setTextContent("InWebContent.jsp");
    element.setUserData("location", new DocumentLocation(2, 1), null);
    root.appendChild(element);

    Element element2 = document.createElement("jsp-file");
    element2.setTextContent("InSrc.jsp");
    element2.setUserData("location", new DocumentLocation(3, 1), null);
    root.appendChild(element2);

    Element element3 = document.createElement("jsp-file");
    element3.setTextContent("DoesNotExist.jsp");
    element3.setUserData("location", new DocumentLocation(4, 1), null);
    root.appendChild(element3);
    document.appendChild(root);

    WebXmlValidator validator = new WebXmlValidator();
    ArrayList<BannedElement> blacklist = validator.checkForElements(resource, document);

    assertEquals(1, blacklist.size());
    String markerId = "com.google.cloud.tools.eclipse.appengine.validation.jspFileMarker";
    assertEquals(markerId, blacklist.get(0).getMarkerId());
    assertEquals("DoesNotExist.jsp could not be resolved", blacklist.get(0).getMessage());
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
