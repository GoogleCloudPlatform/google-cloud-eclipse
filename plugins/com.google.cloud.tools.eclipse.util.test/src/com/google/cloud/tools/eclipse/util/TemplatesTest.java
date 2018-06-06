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

package com.google.cloud.tools.eclipse.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class TemplatesTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private final IProgressMonitor monitor = new NullProgressMonitor();
  private IProject project;
  private String fileLocation;
  private final Map<String, String> dataMap = new HashMap<>();

  @Before
  public void setUp() throws CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    project = workspace.getRoot().getProject("foo");
    project.create(monitor);
    project.open(monitor);

    fileLocation = tempFolder.getRoot().toString() + "/testfile";
  }

  @After
  public void cleanUp() throws CoreException {
    project.delete(true, monitor);
  }

  @Test
  public void testCreateFileContent_appengineWebXml() throws CoreException, IOException {
    Templates.createFileContent(fileLocation, Templates.APPENGINE_WEB_XML_TEMPLATE, dataMap);

    compareToFile("appengineWebXml.txt");
  }

  @Test
  public void testCreateFileContent_appengineWebXmlWithService()
      throws CoreException, IOException {
    dataMap.put("service", "foobar");
    Templates.createFileContent(fileLocation, Templates.APPENGINE_WEB_XML_TEMPLATE, dataMap);

    compareToFile("appengineWebXmlWithService.txt");
  }
  
  @Test
  public void testCreateFileContent_appengineWebXmlWithRuntime()
      throws CoreException, IOException {
    dataMap.put("runtime", "java8");
    Templates.createFileContent(fileLocation, Templates.APPENGINE_WEB_XML_TEMPLATE, dataMap);

    compareToFile("appEngineWebXmlWithRuntime.txt");
  }


  @Test
  public void testCreateFileContent_appYamlWithService()
      throws CoreException, IOException {
    dataMap.put("service", "foobar");
    Templates.createFileContent(fileLocation, Templates.APP_YAML_TEMPLATE, dataMap);

    compareToFile("appYamlWithService.txt");
  }

  @Test
  public void testCreateFileContent_helloAppEngineWithPackage() throws CoreException, IOException {
    dataMap.put("package", "com.example");
    dataMap.put("servletVersion", "2.5");
    Templates.createFileContent(fileLocation, Templates.HELLO_APPENGINE_TEMPLATE, dataMap);

    compareToFile("helloAppEngineWithPackage.txt");
  }

  @Test
  public void testCreateFileContent_helloAppEngineWithoutPackage()
      throws CoreException, IOException {
    dataMap.put("package", "");
    dataMap.put("servletVersion", "2.5");
    Templates.createFileContent(fileLocation, Templates.HELLO_APPENGINE_TEMPLATE, dataMap);

    compareToFile("helloAppEngineWithoutPackage.txt");
  }

  @Test
  public void testCreateFileContent_index() throws CoreException, IOException {
    Templates.createFileContent(fileLocation,
        Templates.INDEX_HTML_TEMPLATE, Collections.<String, String>emptyMap());

    compareToFile("index.txt");
  }

  @Test
  public void testCreateFileContent_web25() throws CoreException, IOException {
    dataMap.put("package", "com.example.");
    dataMap.put("servletVersion", "2.5");
    dataMap.put("namespace", "http://java.sun.com/xml/ns/javaee");
    dataMap.put("schemaUrl", "http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd");
    Templates.createFileContent(fileLocation, Templates.WEB_XML_TEMPLATE, dataMap);

    compareToFile("web25.txt");
  }

  @Test
  public void testCreateFileContent_web25ObjectifyFilter() throws CoreException, IOException {
    dataMap.put("package", "com.example.");
    dataMap.put("servletVersion", "2.5");
    dataMap.put("namespace", "http://java.sun.com/xml/ns/javaee");
    dataMap.put("schemaUrl", "http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd");
    dataMap.put("objectifyAdded", "true");
    Templates.createFileContent(fileLocation, Templates.WEB_XML_TEMPLATE, dataMap);

    compareToFile("web25ObjectifyFilter.txt");
  }

  @Test
  public void testCreateFileContent_web31() throws CoreException, IOException {
    dataMap.put("package", "com.example.");
    dataMap.put("servletVersion", "3.1");
    dataMap.put("namespace", "http://xmlns.jcp.org/xml/ns/javaee");
    dataMap.put("schemaUrl", "http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd");
    Templates.createFileContent(fileLocation, Templates.WEB_XML_TEMPLATE, dataMap);

    compareToFile("web31.txt");
  }

  @Test
  public void testCreateFileContent_noObjectifyFilterForWeb31() throws CoreException, IOException {
    dataMap.put("package", "com.example.");
    dataMap.put("servletVersion", "3.1");
    dataMap.put("namespace", "http://xmlns.jcp.org/xml/ns/javaee");
    dataMap.put("schemaUrl", "http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd");
    dataMap.put("objectifyAdded", "true");
    Templates.createFileContent(fileLocation, Templates.WEB_XML_TEMPLATE, dataMap);

    compareToFile("web31.txt");
  }
  
  @Test
  public void testCreateFileContent_Java8Servlet() throws CoreException, IOException {
    dataMap.put("package", "com.example");
    dataMap.put("servletVersion", "3.1");
    dataMap.put("namespace", "http://xmlns.jcp.org/xml/ns/javaee");
    dataMap.put("schemaUrl", "http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd");
    Templates.createFileContent(fileLocation, Templates.HELLO_APPENGINE_TEMPLATE, dataMap);

    compareToFile("helloAppEngineJava8.txt");
  }

  @Test
  public void testCreateFileContent_loggingProperties() throws CoreException, IOException {
    Templates.createFileContent(fileLocation, Templates.LOGGING_PROPERTIES_TEMPLATE, dataMap);

    compareToFile("loggingProperties.txt");
  }

  @Test
  public void testCreateFileContent_objectifyWebFilterWithPackage()
      throws CoreException, IOException {
    dataMap.put("package", "com.example");
    Templates.createFileContent(fileLocation, Templates.OBJECTIFY_WEB_FILTER_TEMPLATE, dataMap);

    compareToFile("objectifyWebFilterWithPackage.txt");
  }

  @Test
  public void testCreateFileContent_objectifyWebFilterWithoutPackage()
      throws CoreException, IOException {
    dataMap.put("package", "");
    Templates.createFileContent(fileLocation, Templates.OBJECTIFY_WEB_FILTER_TEMPLATE, dataMap);

    compareToFile("objectifyWebFilterWithoutPackage.txt");
  }

  private static InputStream getDataFile(String fileName) throws IOException {
    Bundle bundle = FrameworkUtil.getBundle(TemplatesTest.class);
    URL expectedFileUrl = bundle.getResource("/testData/templates/appengine/" + fileName);
    if (expectedFileUrl == null) {
      throw new IOException("Could not find comparison file " + fileName);
    }
    return expectedFileUrl.openStream();
  }

  private void compareToFile(String expected) throws IOException {

    try (InputStream testFileStream = Files.newInputStream(Paths.get(fileLocation));
        InputStream expectedFileStream = getDataFile(expected);
        Scanner expectedScanner = new Scanner(expectedFileStream);
        Scanner actualScanner = new Scanner(testFileStream)) {
      String expectedContent = expectedScanner.useDelimiter("\\Z").next();
      String actualContent = actualScanner.useDelimiter("\\Z").next();
      Assert.assertEquals(expectedContent, actualContent);
    }
  }

}
