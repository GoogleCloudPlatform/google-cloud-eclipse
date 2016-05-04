package com.google.cloud.tools.eclipse.appengine.newproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.w3c.dom.Node;
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
  public void testMaterialize() 
      throws CoreException, ParserConfigurationException, SAXException, IOException {
    AppEngineStandardProjectConfig config = new AppEngineStandardProjectConfig();
    config.setAppEngineProjectId("TheProjectID");
    
    CodeTemplates.materialize(project, config, monitor);
    
    IFolder src = project.getFolder("src");
    IFile pom = project.getFile("pom.xml");
    Element project = buildDocument(pom).getDocumentElement();
    Node appId = project.getElementsByTagName("app.id").item(0);
    Assert.assertEquals("TheProjectID", appId.getTextContent());
    
    IFolder main = src.getFolder("main");
    IFolder java = main.getFolder("java");
    IFile servlet = java.getFile("HelloAppEngine.java");
    Assert.assertTrue(servlet.exists());
    
    IFolder webapp = main.getFolder("webapp");
    IFolder webinf = webapp.getFolder("WEB-INF");
    IFile appengineWebXml = webinf.getFile("appengine-web.xml");
    Assert.assertTrue(appengineWebXml.exists());
    Document doc = buildDocument(appengineWebXml);
    NodeList applicationElements = doc.getDocumentElement().getElementsByTagName("application");
    Assert.assertEquals("Must have exactly one application", 1, applicationElements.getLength());
    String projectId = applicationElements.item(0).getTextContent();
    Assert.assertEquals("TheProjectID", projectId);
    
    IFile webXml = webinf.getFile("web.xml");
    Element root = buildDocument(webXml).getDocumentElement();
    Assert.assertEquals("web-app", root.getNodeName());
    // Oracle keeps changing the namespace URI in new versions of Java and JEE.
    // This is the namespace URI that currently (Q2 2016) works in App Engine.
    Assert.assertEquals("http://java.sun.com/xml/ns/javaee", root.getNamespaceURI());
    Assert.assertEquals("2.5", root.getAttribute("version"));
    Element servletClass = (Element) root.getElementsByTagName("servlet-class").item(0);
    Assert.assertEquals("HelloAppEngine", servletClass.getTextContent());
    
    IFile htmlFile = webapp.getFile("index.html");
    Element html = buildDocument(htmlFile).getDocumentElement();
    Assert.assertEquals("html", html.getNodeName());
  }
  
  @Test
  public void testNoProjectId() 
      throws CoreException, ParserConfigurationException, SAXException, IOException {
 
    CodeTemplates.materialize(project, new AppEngineStandardProjectConfig(), monitor);
    
    IFolder src = project.getFolder("src");
    IFolder webinf = src.getFolder("main").getFolder("webapp").getFolder("WEB-INF");
    IFile appengineWebXml = webinf.getFile("appengine-web.xml");
    Document doc = buildDocument(appengineWebXml);
    NodeList applicationElements = doc.getDocumentElement().getElementsByTagName("application");
    Assert.assertEquals("Must have exactly one application", 1, applicationElements.getLength());
    String projectId = applicationElements.item(0).getTextContent();
    Assert.assertEquals("", projectId);
  }

  private Document buildDocument(IFile appengineWebXml)
      throws ParserConfigurationException, SAXException, IOException, CoreException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
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
    IFile child = CodeTemplates.createChildFile("web.xml", parent, monitor);
    Assert.assertTrue(child.exists());
    Assert.assertEquals("web.xml", child.getName());
    InputStream in = child.getContents(true);
    Assert.assertNotEquals("File is empty", -1, in.read());
  }
  
  @Test
  public void testCreateChildFileWithTemplates() throws CoreException, IOException {
    Map<String, String> values = new HashMap<>();
    values.put("Package", "package com.google.foo.bar;");
    
    IFile child = CodeTemplates.createChildFile("HelloAppEngine.java", parent, monitor, values);
    Assert.assertTrue(child.exists());
    Assert.assertEquals("HelloAppEngine.java", child.getName());
    InputStream in = child.getContents(true);
    BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
    Assert.assertEquals("package com.google.foo.bar;", reader.readLine());
    Assert.assertEquals("", reader.readLine());
  }

}
