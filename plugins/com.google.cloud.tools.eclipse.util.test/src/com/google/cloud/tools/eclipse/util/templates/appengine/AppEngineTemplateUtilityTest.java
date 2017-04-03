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

package com.google.cloud.tools.eclipse.util.templates.appengine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.eclipse.core.resources.IFile;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class AppEngineTemplateUtilityTest {

  private SubMonitor monitor = SubMonitor.convert(new NullProgressMonitor());
  private IProject project;
  private IFile testFile;
  private String fileLocation;
  private Map<String, String> dataMap = new HashMap<>();


  @Before
  public void setUp() throws CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    project = workspace.getRoot().getProject("foo");
    if (!project.exists()) {
      project.create(monitor);
      project.open(monitor);
    }
    testFile = project.getFile("bar");
    if (!testFile.exists()) {
      testFile.create(new ByteArrayInputStream(new byte[0]), true, monitor);
    }
    fileLocation = testFile.getLocation().toString();
  }

  @After
  public void cleanUp() throws CoreException {
    testFile.delete(true, monitor);
    project.delete(true, monitor);
  }

  @Test
  public void testCreateFileContent_appengineWebXml() throws CoreException, IOException {
    AppEngineTemplateUtility.createFileContent(fileLocation,
        AppEngineTemplateUtility.APPENGINE_WEB_XML_TEMPLATE,
        dataMap);

    compareToFile("appengineWebXml.txt");
  }
  
  @Test
  public void testCreateFileContent_appengineWebXmlWithService()
      throws CoreException, IOException {
    dataMap.put("service", "foobar");
    AppEngineTemplateUtility.createFileContent(fileLocation,
        AppEngineTemplateUtility.APPENGINE_WEB_XML_TEMPLATE,
        dataMap);

    compareToFile("appengineWebXmlWithService.txt");
  }
  
  @Test
  public void testCreateFileContent_appYamlWithService()
      throws CoreException, IOException {
    dataMap.put("service", "foobar");
    AppEngineTemplateUtility.createFileContent(fileLocation,
        AppEngineTemplateUtility.APP_YAML_TEMPLATE,
        dataMap);

    compareToFile("appYamlWithService.txt");
  }

  @Test
  public void testCreateFileContent_helloAppEngineWithPackage() throws CoreException, IOException {
    dataMap.put("package", "com.example");
    AppEngineTemplateUtility.createFileContent(fileLocation,
        AppEngineTemplateUtility.HELLO_APPENGINE_TEMPLATE, dataMap);

    compareToFile("helloAppEngineWithPackage.txt");
  }

  @Test
  public void testCreateFileContent_helloAppEngineWithoutPackage()
      throws CoreException, IOException {
    dataMap.put("package", "");
    AppEngineTemplateUtility.createFileContent(fileLocation,
        AppEngineTemplateUtility.HELLO_APPENGINE_TEMPLATE, dataMap);

    compareToFile("helloAppEngineWithoutPackage.txt");
  }

  @Test
  public void testCreateFileContent_index() throws CoreException, IOException {
    AppEngineTemplateUtility.createFileContent(fileLocation,
        AppEngineTemplateUtility.INDEX_HTML_TEMPLATE, Collections.<String, String>emptyMap());

    compareToFile("index.txt");
  }

  @Test
  public void testCreateFileContent_web() throws CoreException, IOException {
    dataMap.put("package", "com.example.");
    AppEngineTemplateUtility.createFileContent(fileLocation,
        AppEngineTemplateUtility.WEB_XML_TEMPLATE, dataMap);

    compareToFile("web.txt");
  }

  private static InputStream getDataFile(String fileName) throws IOException {
    Bundle bundle = FrameworkUtil.getBundle(AppEngineTemplateUtilityTest.class);
    URL expectedFileUrl = bundle.getResource("/testData/templates/appengine/" + fileName);
    return expectedFileUrl.openStream();
  }

  private void compareToFile(String expected) throws CoreException, IOException {
    
    try (InputStream testFileStream = testFile.getContents(true);
        InputStream expectedFileStream = getDataFile(expected);
        Scanner expectedScanner = new Scanner(expectedFileStream);
        Scanner actualScanner = new Scanner(testFileStream)) {
      String expectedContent = expectedScanner.useDelimiter("\\Z").next();
      String actualContent = actualScanner.useDelimiter("\\Z").next();
      Assert.assertEquals(expectedContent, actualContent);
    }
  }

}
