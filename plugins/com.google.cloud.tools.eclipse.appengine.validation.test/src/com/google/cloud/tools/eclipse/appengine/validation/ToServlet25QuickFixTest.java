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

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;

/**
 * Must be run as a plugin test.
 */
public class ToServlet25QuickFixTest {
    
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  private ToServlet25QuickFix fix = new ToServlet25QuickFix();

  @Test 
  public void testGetLabel() {
    assertEquals("Convert to Java Servlet API 2.5", fix.getLabel());
  }
  
  @Test
  public void testConvertServlet_jcpNamespace() throws IOException, ParserConfigurationException,
      SAXException, TransformerException, CoreException {
    String webXml = "<web-app xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" version='3.1'>"
        + "<foo></foo></web-app>";
    Document transformed = transform(webXml);
    Element documentElement = transformed.getDocumentElement();
    assertEquals("2.5", documentElement.getAttribute("version"));
    Element element = (Element) documentElement
        .getElementsByTagNameNS("http://java.sun.com/xml/ns/javaee", "foo").item(0);
    assertEquals("foo", element.getTagName());
  }
  
  
  @Test
  public void testConvertServlet_sunNamespace() throws IOException, ParserConfigurationException,
      SAXException, TransformerException, CoreException {
    String webXml = "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" version='3.0'>"
        + "<foo></foo></web-app>";
    Document transformed = transform(webXml);
    Element documentElement = transformed.getDocumentElement();
    assertEquals("2.5", documentElement.getAttribute("version"));
    Element element = (Element) documentElement
        .getElementsByTagNameNS("http://java.sun.com/xml/ns/javaee", "foo").item(0);
    assertEquals("foo", element.getTagName());
  }
  
  private Document transform(String webXml)
      throws CoreException, ParserConfigurationException, SAXException, IOException {
    IProject project = projectCreator.getProject();
    IFile file = project.getFile("testdata.xml");
    file.create(ValidationTestUtils.stringToInputStream(webXml), IFile.FORCE, null);
    
    IMarker marker = Mockito.mock(IMarker.class);
    Mockito.when(marker.getResource()).thenReturn(file);
        
    fix.run(marker);

    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    builderFactory.setNamespaceAware(true);
    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    InputStream contents = file.getContents();
    return builder.parse(contents);
  }

}
