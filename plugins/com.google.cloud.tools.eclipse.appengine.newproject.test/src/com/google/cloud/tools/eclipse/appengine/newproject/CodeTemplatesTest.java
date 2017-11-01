/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.newproject;

import static org.junit.Assert.assertFalse;

import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectConfig.Template;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineRuntime;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.Templates;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CodeTemplatesTest {

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  private IProgressMonitor monitor = new NullProgressMonitor();
  private IFolder parent;
  private IProject project;

  @Before
  public void setUp() throws CoreException {
    project = projectCreator.getProject();
    parent = project.getFolder("testfolder");
    parent.create(true, true, monitor);
  }

  @Test
  public void testMaterializeAppEngineStandardFiles()
      throws CoreException, ParserConfigurationException, SAXException, IOException {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    IFile mostImportant = CodeTemplates.materializeAppEngineStandardFiles(project, config, monitor);
    validateNonConfigFiles(mostImportant, "http://java.sun.com/xml/ns/javaee",
        "http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd", "2.5");
    validateAppEngineWebXml(AppEngineRuntime.STANDARD_JAVA_7);
  }

  @Test
  public void testMaterializeAppEngineStandardFiles_java8()
      throws CoreException, ParserConfigurationException, SAXException, IOException {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    config.setRuntimeId(AppEngineRuntime.STANDARD_JAVA_8.getId());
    IFile mostImportant = CodeTemplates.materializeAppEngineStandardFiles(project, config, monitor);
    validateNonConfigFiles(mostImportant, "http://xmlns.jcp.org/xml/ns/javaee",
        "http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd", "3.1");
    validateAppEngineWebXml(AppEngineRuntime.STANDARD_JAVA_8);
  }

  @Test
  public void testMaterializeAppEngineStandardFiles_noPomXml() throws CoreException {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    CodeTemplates.materializeAppEngineStandardFiles(project, config, monitor);
    assertFalse(project.getFile("pom.xml").exists());
  }

  @Test
  public void testMaterializeAppEngineStandardFiles_pomXmlIfEnablingMaven()
      throws CoreException, ParserConfigurationException, SAXException, IOException {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    config.setUseMaven("my.project.group.id", "my-project-artifact-id", "98.76.54");
    CodeTemplates.materializeAppEngineStandardFiles(project, config, monitor);
    validatePomXml();
  }

  @Test
  public void testMaterializeAppEngineFlexFiles()
      throws CoreException, ParserConfigurationException, SAXException, IOException {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    config.setServiceName("database-service");
    IFile mostImportant = CodeTemplates.materializeAppEngineFlexFiles(project, config, monitor);
    validateNonConfigFiles(mostImportant, "http://xmlns.jcp.org/xml/ns/javaee",
        "http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd", "3.1");
    validateAppYaml();
  }

  @Test
  public void testMaterializeAppEngineFlexFiles_noPomXml() throws CoreException {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    CodeTemplates.materializeAppEngineFlexFiles(project, config, monitor);
    assertFalse(project.getFile("pom.xml").exists());
  }

  @Test
  public void testMaterializeAppEngineFlexFiles_pomXmlIfEnablingMaven()
      throws CoreException, ParserConfigurationException, SAXException, IOException {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    config.setUseMaven("my.project.group.id", "my-project-artifact-id", "98.76.54");
    CodeTemplates.materializeAppEngineFlexFiles(project, config, monitor);
    validatePomXml();
  }

  @Test
  public void testMaterializeAppEngineFlexJarFiles()
      throws CoreException, ParserConfigurationException, SAXException, IOException  {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    config.setUseMaven("my.project.group.id", "my-project-artifact-id", "98.76.54");
    config.setServiceName("database-service");

    IFile mostImportant = CodeTemplates.materializeAppEngineFlexJarFiles(project, config, monitor);

    validateFlexJarNonConfigFiles(mostImportant);
    validateAppYaml();
    validatePomXml();
  }

  @Test
  public void testMaterializeFlexSpringBoot()
      throws CoreException, ParserConfigurationException, SAXException, IOException  {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    config.setUseMaven("my.project.group.id", "my-project-artifact-id", "98.76.54");
    config.setServiceName("database-service");
    config.setTemplate(Template.SPRING_BOOT);

    IFile mostImportant = CodeTemplates.materializeAppEngineFlexJarFiles(project, config, monitor);

    validateFlexSpringBootNonConfigFiles(mostImportant);
    validateAppYaml();
    validatePomXml();
  }

  @Test
  public void testMaterializeAppEngineFlexJarFiles_mainClassSet()
      throws CoreException, ParserConfigurationException, SAXException, IOException  {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    config.setUseMaven("my.project.group.id", "my-project-artifact-id", "98.76.54");
    config.setPackageName("com.example");

    IFile mostImportant = CodeTemplates.materializeAppEngineFlexJarFiles(project, config, monitor);
    validateMainClassInPomXml("com.example", mostImportant);
  }

  @Test
  public void testMaterializeAppEngineFlexJarFiles_defaultPackageMainClassSet()
      throws CoreException, ParserConfigurationException, SAXException, IOException  {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    config.setUseMaven("my.project.group.id", "my-project-artifact-id", "98.76.54");

    IFile mostImportant = CodeTemplates.materializeAppEngineFlexJarFiles(project, config, monitor);
    validateMainClassInPomXml(null /* expectedPackage */, mostImportant);
  }

  private void validateMainClassInPomXml(String expectedPackage, IFile mostImportant)
      throws ParserConfigurationException, SAXException, IOException, CoreException {
    String mainClassName = mostImportant.getFullPath().removeFileExtension().lastSegment();

    IFile pomXml = project.getFile("pom.xml");
    Element root = buildDocument(pomXml).getDocumentElement();
    Element mainClass = (Element) root.getElementsByTagName("mainClass").item(0);
    if (expectedPackage == null) {
      Assert.assertEquals(mainClassName, mainClass.getTextContent());
    } else {
      Assert.assertEquals(expectedPackage + "." + mainClassName, mainClass.getTextContent());
    }
  }

  private void validateNonConfigFiles(IFile mostImportant,
      String webXmlNamespace, String webXmlSchemaUrl, String servletVersion)
      throws ParserConfigurationException, SAXException, IOException, CoreException {
    IFile servlet = project.getFile("src/main/java/HelloAppEngine.java");
    Assert.assertTrue(servlet.exists());
    Assert.assertEquals(servlet, mostImportant);

    IFile webXml = project.getFile("src/main/webapp/WEB-INF/web.xml");
    Element root = buildDocument(webXml).getDocumentElement();
    Assert.assertEquals("web-app", root.getNodeName());
    Assert.assertEquals(webXmlNamespace, root.getNamespaceURI());
    Assert.assertEquals(webXmlNamespace + " " + webXmlSchemaUrl,
        root.getAttribute("xsi:schemaLocation"));
    Assert.assertEquals(servletVersion, root.getAttribute("version"));
    Element servletClass = (Element) root.getElementsByTagName("servlet-class").item(0);
    if (servletClass != null) { // servlet 2.5
      Assert.assertEquals("HelloAppEngine", servletClass.getTextContent());
    }
    
    IFile htmlFile = project.getFile("src/main/webapp/index.html");
    Element html = buildDocument(htmlFile).getDocumentElement();
    Assert.assertEquals("html", html.getNodeName());

    IFile servletTest = project.getFile("src/test/java/HelloAppEngineTest.java");
    Assert.assertTrue(servletTest.exists());
    IFile mockServletResponse = project.getFile("src/test/java/MockHttpServletResponse.java");
    Assert.assertTrue(mockServletResponse.exists());
  }

  private void validateFlexJarNonConfigFiles(IFile mostImportant) {
    IFile main = project.getFile("src/main/java/HelloAppEngineMain.java");
    Assert.assertTrue(main.exists());
    Assert.assertEquals(main, mostImportant);
    IFile handler = project.getFile("src/main/java/HelloAppEngineHandler.java");
    Assert.assertTrue(handler.exists());

    IFile handlerTest = project.getFile("src/test/java/HelloAppEngineHandlerTest.java");
    Assert.assertTrue(handlerTest.exists());
    IFile mockHttpExchange = project.getFile("src/test/java/MockHttpExchange.java");
    Assert.assertTrue(mockHttpExchange.exists());
  }

  private void validateFlexSpringBootNonConfigFiles(IFile mostImportant) {
    IFile main = project.getFile("src/main/java/HelloAppEngineSpringBoot.java");
    Assert.assertTrue(main.exists());
    Assert.assertEquals(main, mostImportant);
    IFile handler = project.getFile("src/test/java/HelloAppEngineSpringBootIntegrationTest.java");
    Assert.assertTrue(handler.exists());
  }

  private void validateAppEngineWebXml(AppEngineRuntime runtime)
      throws ParserConfigurationException, SAXException, IOException, CoreException {
    IFolder webinf = project.getFolder("src/main/webapp/WEB-INF");
    IFile appengineWebXml = webinf.getFile("appengine-web.xml");
    Assert.assertTrue(appengineWebXml.exists());
    Document doc = buildDocument(appengineWebXml);
    NodeList threadsafeElements = doc.getDocumentElement().getElementsByTagName("threadsafe");
    Assert.assertEquals("Must have exactly one threadsafe", 1, threadsafeElements.getLength());
    String threadsafe = threadsafeElements.item(0).getTextContent();
    Assert.assertEquals("true", threadsafe);
    NodeList sessionsEnabledElements
        = doc.getDocumentElement().getElementsByTagName("sessions-enabled");
    Assert.assertEquals("Must have exactly one sessions-enabled",
        1, sessionsEnabledElements.getLength());
    String sessionsEnabled = sessionsEnabledElements.item(0).getTextContent();
    Assert.assertEquals("false", sessionsEnabled);

    NodeList runtimeElements = doc.getDocumentElement().getElementsByTagName("runtime");
    if (runtime.getId() == null) {
      Assert.assertEquals("should not have a <runtime> element", 0, runtimeElements.getLength());
    } else {
      Assert.assertEquals("should have exactly 1 <runtime> element", 1,
          runtimeElements.getLength());
      Assert.assertEquals(runtime.getId(), runtimeElements.item(0).getTextContent());
    }
  }

  private void validateAppYaml() throws IOException, CoreException {
    IFile appYaml = project.getFile("src/main/appengine/app.yaml");
    Assert.assertTrue(appYaml.exists());

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(appYaml.getContents(), StandardCharsets.UTF_8))) {
      Assert.assertEquals("runtime: java", reader.readLine());
      Assert.assertEquals("env: flex", reader.readLine());
      Assert.assertEquals("service: database-service", reader.readLine());
    }
  }

  private void validatePomXml()
      throws ParserConfigurationException, SAXException, IOException, CoreException {
    IFile pomXml = project.getFile("pom.xml");
    Element root = buildDocument(pomXml).getDocumentElement();
    Assert.assertEquals("project", root.getNodeName());
    Assert.assertEquals("http://maven.apache.org/POM/4.0.0", root.getNamespaceURI());
    Assert.assertEquals(
        "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd",
        root.getAttribute("xsi:schemaLocation"));

    Element groupId = (Element) root.getElementsByTagName("groupId").item(0);
    Assert.assertEquals("my.project.group.id", groupId.getTextContent());
    Element artifactId = (Element) root.getElementsByTagName("artifactId").item(0);
    Assert.assertEquals("my-project-artifact-id", artifactId.getTextContent());
    Element version = (Element) root.getElementsByTagName("version").item(0);
    Assert.assertEquals("98.76.54", version.getTextContent());
  }

  private Document buildDocument(IFile xml)
      throws ParserConfigurationException, SAXException, IOException, CoreException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(false);
    factory.setExpandEntityReferences(false);
    factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
    factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(xml.getContents());
    return doc;
  }

  @Test
  public void testCreateChildFile() throws CoreException, IOException {
    Map<String, String> values = new HashMap<>();
    values.put("package", "com.google.foo.bar");
    values.put("servletVersion", "2.5");

    IFile child = CodeTemplates.createChildFile("HelloAppEngine.java",
        Templates.HELLO_APPENGINE_TEMPLATE, parent, values, monitor);
    Assert.assertTrue(child.exists());
    Assert.assertEquals("HelloAppEngine.java", child.getName());
    try (InputStream in = child.getContents(true);
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(in, StandardCharsets.UTF_8))) {
      Assert.assertEquals("package com.google.foo.bar;", reader.readLine());
      Assert.assertEquals("", reader.readLine());
    }
  }

  @Test
  public void testCopyChildFile() throws CoreException {
    CodeTemplates.copyChildFile("favicon.ico", parent, monitor);
    Assert.assertTrue(parent.getFile("favicon.ico").exists());
  }
}
