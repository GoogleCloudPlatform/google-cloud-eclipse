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

package com.google.cloud.tools.eclipse.appengine.libraries;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;

public class PomTest {
  
  @Rule public final TestProjectCreator projectCreator = new TestProjectCreator();

  // todo we're doing enough of this we should import or write some utilities
  private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
  
  @BeforeClass 
  public static void configureParser() {
    factory.setNamespaceAware(true);
  }
  
  @Test
  public void testAddDependencies()
      throws CoreException, SAXException, IOException, ParserConfigurationException {
    
    IProject project = projectCreator.getProject();
    IFile pomFile = project.getFile("pom.xml");
    
    InputStream in = new FileInputStream(
        Paths.get("testdata/testpom.xml").toAbsolutePath().toFile());
    pomFile.create(in, IFile.FORCE, null);
    
    Pom pom = Pom.parse(pomFile);
    List<Library> libraries = AppEngineLibraries.getAvailableLibraries();
    
    LibraryFile file0 = new LibraryFile(new MavenCoordinates("com.example.group0", "artifact0"));
    List<LibraryFile> list0 = new ArrayList<>();
    list0.add(file0);
    
    List<LibraryFile> list1 = new ArrayList<>();
    LibraryFile file1 = new LibraryFile(new MavenCoordinates("com.example.group1", "artifact1"));
    list1.add(file1);
    
    List<LibraryFile> list2 = new ArrayList<>();
    LibraryFile file2 = new LibraryFile(new MavenCoordinates("com.example.group2", "artifact2"));
    LibraryFile file3 = new LibraryFile(new MavenCoordinates("com.example.group3", "artifact3"));
    list2.add(file2);
    list2.add(file3);
    
    libraries.get(0).setLibraryFiles(list0 );
    libraries.get(1).setLibraryFiles(list1);
    libraries.get(2).setLibraryFiles(list2);
    
    pom.addDependencies(libraries);
    
    InputStream contents = pomFile.getContents();
    Document actual = debuggableParse(contents);
    
    NodeList dependencies = actual.getElementsByTagName("dependencies");
    Assert.assertEquals(1, dependencies.getLength());
    NodeList children = ((Element) dependencies.item(0)).getElementsByTagName("dependency");
    
    Assert.assertEquals(4, children.getLength());
    
    Element child0 = (Element) children.item(0);
    Element groupId = getOnlyChild(child0, "groupId");
    Assert.assertEquals("com.example.group0", groupId.getTextContent());
    Element artifactId = getOnlyChild(child0, "artifactId");
    Assert.assertEquals("artifact0", artifactId.getTextContent());
    
    Element child3 = (Element) children.item(3);
    Element groupId3 = getOnlyChild(child3, "groupId");
    Assert.assertEquals("com.example.group3", groupId3.getTextContent());
    Element artifactId3 = getOnlyChild(child3, "artifactId");
    Assert.assertEquals("artifact3", artifactId3.getTextContent());
  }

  /**
   * For ease of debugging this method stores the entire file into a string, then parses
   * that string. 
   */
  private static Document debuggableParse(InputStream in)
      throws ParserConfigurationException, IOException, UnsupportedEncodingException, SAXException {
    DocumentBuilder builder = factory.newDocumentBuilder();
    
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (int i = in.read(); i != -1; i = in.read()) {
      out.write(i);
    }
    
    byte[] data = out.toByteArray();
    String xml = new String(data, "UTF-8");
    System.err.println(xml);
    Document actual = builder.parse(new ByteArrayInputStream(data));
    return actual;
  }

  private static Element getOnlyChild(Element element, String name) {
    NodeList children = element.getElementsByTagName(name);
    if (children.getLength() == 0) {
      Assert.fail("No element " + name);
    }
    Assert.assertEquals("More than one " + name, 1, children.getLength());
    return (Element) children.item(0);
  }

}
