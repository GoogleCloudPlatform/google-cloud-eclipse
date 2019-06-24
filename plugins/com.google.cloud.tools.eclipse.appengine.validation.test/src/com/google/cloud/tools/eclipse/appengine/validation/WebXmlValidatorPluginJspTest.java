/*
 * Copyright 2018 Google LLC
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WebXmlValidatorPluginJspTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator().withFacets(
      JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25);

  private IProject project;

  @Before
  public void setUp() throws CoreException {
    project = projectCreator.getProject();

    createFile(project, "src/main/java", "ServletClass.java", "public class ServletClass {}");
    createFile(project, "WebContent", "InWebContent.jsp", "");
    createFile(project, "src", "InSrc.jsp", "");
  }

  @After
  public void tearDown() throws CoreException {
    // Delete to avoid https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1916
    project.getProject().getFile("WebContent/InWebContent.jsp").delete(true, null);
    project.getProject().getFile("src/InSrc.jsp").delete(true, null);
  }

  private static void createFile(IProject project, String folder, String filename,
      String contents) throws CoreException {
    ResourceUtils.createFolders(project.getFolder(folder), null);
    IFile file = project.getFile(folder + "/" + filename);
    file.create(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)), true, null);
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

    IFile webXml = project.getFile("WebContent/WEB-INF/web.xml");
    WebXmlValidator validator = new WebXmlValidator();
    ArrayList<ElementProblem> problems = validator.checkForProblems(webXml, document);

    assertEquals(1, problems.size());
    String markerId = "com.google.cloud.tools.eclipse.appengine.validation.jspFileMarker";
    assertEquals(markerId, problems.get(0).getMarkerId());
    assertEquals("DoesNotExist.jsp could not be resolved", problems.get(0).getMessage());
  }
}
