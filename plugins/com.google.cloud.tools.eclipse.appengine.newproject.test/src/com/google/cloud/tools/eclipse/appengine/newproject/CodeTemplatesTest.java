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

import com.google.cloud.tools.eclipse.util.templates.appengine.AppEngineTemplateUtility;
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
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CodeTemplatesTest {

  private SubMonitor monitor = SubMonitor.convert(new NullProgressMonitor());
  private IFolder parent;
  private IProject project;
  
  @Before
  public void setUp() throws CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    project = workspace.getRoot().getProject("foobar");
    if (!project.exists()) {
      project.create(monitor);
      project.open(monitor);
    }
    parent = project.getFolder("testfolder");
    if (!parent.exists()) {
      parent.create(true, true, monitor);
    }
  }
  
  @After
  public void cleanUp() throws CoreException {
    parent.delete(true, monitor);
    project.delete(true, monitor);
  }

  @Test
  public void testMaterializeAppEngineStandardFiles()
      throws CoreException, ParserConfigurationException, SAXException, IOException {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    IFile mostImportant = CodeTemplates.materializeAppEngineStandardFiles(project, config, monitor);
    validateNonConfigFiles(mostImportant);
    validateAppEngineWebXml();
  }

  @Test
  public void testMaterializeAppEngineFlexFiles()
      throws CoreException, ParserConfigurationException, SAXException, IOException {
    AppEngineProjectConfig config = new AppEngineProjectConfig();
    IFile mostImportant = CodeTemplates.materializeAppEngineFlexFiles(project, config, monitor);
    validateNonConfigFiles(mostImportant);
    validateAppYaml();
  }

  private void validateNonConfigFiles(IFile mostImportant)
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
    // Oracle keeps changing the namespace URI in new versions of Java and JEE.
    // This is the namespace URI that works in App Engine standard.
    Assert.assertEquals("http://java.sun.com/xml/ns/javaee", root.getNamespaceURI());
    Assert.assertEquals("2.5", root.getAttribute("version"));
    Element servletClass = (Element) root.getElementsByTagName("servlet-class").item(0);
    Assert.assertEquals("HelloAppEngine", servletClass.getTextContent());
    
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

  private void validateAppEngineWebXml()
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

  private Document buildDocument(IFile appengineWebXml)
      throws ParserConfigurationException, SAXException, IOException, CoreException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(false);
    factory.setExpandEntityReferences(false);
    factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
    factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(appengineWebXml.getContents());
    return doc;
  }
  
  @Test
  public void testCreateChildFolder() throws CoreException {
    IFolder child = CodeTemplates.createChildFolder("testchild", parent, monitor);
    Assert.assertTrue(child.exists());
    Assert.assertEquals("testchild", child.getName());
  }

  @Test
  public void testCreateChildFile() throws CoreException, IOException {
    Map<String, String> values = new HashMap<>();
    values.put("package", "com.google.foo.bar");
    
    IFile child = CodeTemplates.createChildFile("HelloAppEngine.java", 
        AppEngineTemplateUtility.HELLO_APPENGINE_TEMPLATE, parent, values, monitor);
    Assert.assertTrue(child.exists());
    Assert.assertEquals("HelloAppEngine.java", child.getName());
    InputStream in = child.getContents(true);
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(in, StandardCharsets.UTF_8.name()))) {
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
