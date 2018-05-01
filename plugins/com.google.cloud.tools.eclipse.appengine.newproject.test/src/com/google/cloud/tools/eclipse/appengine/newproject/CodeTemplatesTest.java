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

import com.google.cloud.tools.eclipse.appengine.ui.AppEngineRuntime;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.MappedNamespaceContext;
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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
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
  public void testMaterializeAppEngineStandardFiles_pomXmlIfEnablingMaven() throws CoreException,
      ParserConfigurationException, SAXException, IOException, XPathExpressionException {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    config.setUseMaven("my.project.group.id", "my-project-artifact-id", "98.76.54");
    CodeTemplates.materializeAppEngineStandardFiles(project, config, monitor);
    validateStandardPomXml();
  }

  @Test
  public void testMaterializeAppEngineFlexFiles()
      throws CoreException, ParserConfigurationException, SAXException, IOException {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
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
  public void testMaterializeAppEngineFlexFiles_pomXmlIfEnablingMaven() throws CoreException,
      ParserConfigurationException, SAXException, IOException, XPathExpressionException {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    config.setUseMaven("my.project.group.id", "my-project-artifact-id", "98.76.54");
    CodeTemplates.materializeAppEngineFlexFiles(project, config, monitor);
    validateFlexPomXml();
  }

  private void validateNonConfigFiles(IFile mostImportant,
      String webXmlNamespace, String webXmlSchemaUrl, String servletVersion)
      throws ParserConfigurationException, SAXException, IOException, CoreException {
    IFolder src = project.getFolder("src");
    IFolder main = src.getFolder("main");
    IFolder java = main.getFolder("java");
    IFile servlet = java.getFile("HelloAppEngine.java");
    Assert.assertTrue(servlet.exists());
    Assert.assertEquals(servlet, mostImportant);

    IFolder webapp = main.getFolder("webapp");
    IFolder webinf = webapp.getFolder("WEB-INF");
    IFile webXml = webinf.getFile("web.xml");
    Element root = buildDocument(webXml).getDocumentElement();
    Assert.assertEquals("web-app", root.getNodeName());
    Assert.assertEquals(webXmlNamespace, root.getNamespaceURI());
    Assert.assertEquals(webXmlNamespace + " " + webXmlSchemaUrl,
        root.getAttribute("xsi:schemaLocation"));
    Assert.assertEquals(servletVersion, root.getAttribute("version"));
    Element servletClass = (Element) root
        .getElementsByTagNameNS("http://java.sun.com/xml/ns/javaee", "servlet-class").item(0);
    if (servletClass != null) { // servlet 2.5
      Assert.assertEquals("HelloAppEngine", servletClass.getTextContent());
    }
    
    IFile htmlFile = webapp.getFile("index.html");
    Element html = buildDocument(htmlFile).getDocumentElement();
    Assert.assertEquals("html", html.getNodeName());

    IFolder test = src.getFolder("test");
    IFolder testJava = test.getFolder("java");
    IFile servletTest = testJava.getFile("HelloAppEngineTest.java");
    Assert.assertTrue(servletTest.exists());
    IFile mockServletResponse = testJava.getFile("MockHttpServletResponse.java");
    Assert.assertTrue(mockServletResponse.exists());
  }

  private void validateAppEngineWebXml(AppEngineRuntime runtime)
      throws ParserConfigurationException, SAXException, IOException, CoreException {
    IFolder webinf = project.getFolder("src/main/webapp/WEB-INF");
    IFile appengineWebXml = webinf.getFile("appengine-web.xml");
    Assert.assertTrue(appengineWebXml.exists());
    Document doc = buildDocument(appengineWebXml);
    NodeList threadsafeElements = doc.getDocumentElement().getElementsByTagNameNS(
        "http://appengine.google.com/ns/1.0", "threadsafe");
    Assert.assertEquals("Must have exactly one threadsafe", 1, threadsafeElements.getLength());
    String threadsafe = threadsafeElements.item(0).getTextContent();
    Assert.assertEquals("true", threadsafe);
    NodeList sessionsEnabledElements
        = doc.getDocumentElement().getElementsByTagNameNS("http://appengine.google.com/ns/1.0",
            "sessions-enabled");
    Assert.assertEquals("Must have exactly one sessions-enabled",
        1, sessionsEnabledElements.getLength());
    String sessionsEnabled = sessionsEnabledElements.item(0).getTextContent();
    Assert.assertEquals("false", sessionsEnabled);

    NodeList runtimeElements = doc.getDocumentElement().getElementsByTagNameNS(
        "http://appengine.google.com/ns/1.0", "runtime");
    if (runtime.getId() == null) {
      Assert.assertEquals("should not have a <runtime> element", 0, runtimeElements.getLength());
    } else {
      Assert.assertEquals("should have exactly 1 <runtime> element", 1,
          runtimeElements.getLength());
      Assert.assertEquals(runtime.getId(), runtimeElements.item(0).getTextContent());
    }
  }

  private void validateAppYaml() throws IOException, CoreException {
    IFolder appengineFolder = project.getFolder("src/main/appengine");
    Assert.assertTrue(appengineFolder.exists());
    IFile appYaml = appengineFolder.getFile("app.yaml");
    Assert.assertTrue(appYaml.exists());

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(appYaml.getContents(), StandardCharsets.UTF_8))) {
      Assert.assertEquals("runtime: java", reader.readLine());
      Assert.assertEquals("env: flex", reader.readLine());
    }
  }

  private void validateStandardPomXml() throws ParserConfigurationException, SAXException,
      IOException, CoreException, XPathExpressionException {
    Element root = validatePom();
    
    String sdkVersion = root
        .getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "appengine.api.sdk.version")
        .item(0).getTextContent();
    DefaultArtifactVersion sdkArtifactVersion =
        new DefaultArtifactVersion(sdkVersion);
    DefaultArtifactVersion expectedSdk = new DefaultArtifactVersion("1.9.62");
    Assert.assertTrue(sdkVersion, sdkArtifactVersion.compareTo(expectedSdk) >= 0);    
  }

  private Element validatePom() throws ParserConfigurationException, SAXException, IOException,
      CoreException, XPathExpressionException {
    IFile pomXml = project.getFile("pom.xml");
    Element root = buildDocument(pomXml).getDocumentElement();
    Assert.assertEquals("project", root.getNodeName());
    Assert.assertEquals("http://maven.apache.org/POM/4.0.0", root.getNamespaceURI());
    Assert.assertEquals(
        "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd",
        root.getAttribute("xsi:schemaLocation"));

    Element groupId = (Element) root
        .getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "groupId").item(0);
    Assert.assertEquals("my.project.group.id", groupId.getTextContent());
    Element artifactId = (Element) root
        .getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "artifactId").item(0);
    Assert.assertEquals("my-project-artifact-id", artifactId.getTextContent());
    Element version = (Element) root
        .getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "version").item(0);
    Assert.assertEquals("98.76.54", version.getTextContent());

    Element pluginVersion =
        (Element) root.getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0",
            "appengine.maven.plugin.version").item(0);
    DefaultArtifactVersion artifactVersion =
        new DefaultArtifactVersion(pluginVersion.getTextContent());
    DefaultArtifactVersion expected = new DefaultArtifactVersion("1.3.2");
    Assert.assertTrue(artifactVersion.compareTo(expected) >= 0);
    
    XPath xpath = XPathFactory.newInstance().newXPath();
    xpath.setNamespaceContext(new MappedNamespaceContext("m", "http://maven.apache.org/POM/4.0.0"));
    NodeList dependencyManagementNodes = (NodeList) xpath.evaluate(
        "./m:dependencyManagement",
        root,
        XPathConstants.NODESET);
    Assert.assertEquals(1, dependencyManagementNodes.getLength());
    
    String bomGroupId = (String) xpath.evaluate(
        "string(./m:dependencyManagement/m:dependencies/m:dependency/m:groupId)",
        root,
        XPathConstants.STRING);
    Assert.assertEquals("com.google.cloud", bomGroupId);
    String bomArtifactId = (String) xpath.evaluate(
        "string(./m:dependencyManagement/m:dependencies/m:dependency/m:artifactId)",
        root,
        XPathConstants.STRING);
    Assert.assertEquals("google-cloud-bom", bomArtifactId);

    DefaultArtifactVersion bomVersion = new DefaultArtifactVersion((String) xpath.evaluate(
        "string(./m:dependencyManagement/m:dependencies/m:dependency/m:version)",
        root,
        XPathConstants.STRING));
    Assert.assertTrue(
        bomVersion.compareTo(new DefaultArtifactVersion("0.42.0-alpha")) >= 0);

    String scope = (String) xpath.evaluate(
        "string(./m:dependencyManagement/m:dependencies/m:dependency/m:scope)",
        root,
        XPathConstants.STRING);
    Assert.assertEquals("import", scope);
    
    String type = (String) xpath.evaluate(
        "string(./m:dependencyManagement/m:dependencies/m:dependency/m:type)",
        root,
        XPathConstants.STRING);
    Assert.assertEquals("pom", type);
    
    return root;
  }

  private void validateFlexPomXml() throws ParserConfigurationException, SAXException, IOException,
      CoreException, XPathExpressionException {
    validatePom();
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
    return builder.parse(xml.getContents());
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
