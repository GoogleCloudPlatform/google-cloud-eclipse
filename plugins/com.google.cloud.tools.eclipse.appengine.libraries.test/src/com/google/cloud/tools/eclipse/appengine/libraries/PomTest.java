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

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PomTest {
  
  @Rule public final TestProjectCreator projectCreator = new TestProjectCreator();

  // todo we're doing enough of this we should import or write some utilities
  private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
  
  @BeforeClass 
  public static void configureParser() {
    factory.setNamespaceAware(true);
  }
  
  private Pom pom;
  private IFile pomFile;
  
  @Before
  public void setUp() throws SAXException, IOException, CoreException {
    IProject project = projectCreator.getProject();
    pomFile = project.getFile("pom.xml");
    InputStream in = Files.newInputStream(Paths.get("testdata/testpom.xml").toAbsolutePath());
    pomFile.create(in, IFile.FORCE, null);
   
    pom = Pom.parse(pomFile);
  }
  
  @Test
  public void testAddDependencies() 
      throws CoreException, ParserConfigurationException, IOException, SAXException {
    
    MavenCoordinates.Builder builder = new MavenCoordinates.Builder()
        .setGroupId("com.example.group0")
        .setArtifactId("artifact0")
        .setVersion("1.2.3"); 

    MavenCoordinates coordinates0 = builder.build();
    LibraryFile file0 = new LibraryFile(coordinates0);
    List<LibraryFile> list0 = new ArrayList<>();
    list0.add(file0);
    
    MavenCoordinates coordinates1 = new MavenCoordinates.Builder()
        .setGroupId("com.example.group1")
        .setArtifactId("artifact1")
        .build();
    List<LibraryFile> list1 = new ArrayList<>();
    LibraryFile file1 = new LibraryFile(coordinates1);
    list1.add(file1);
    
    MavenCoordinates coordinates2 = new MavenCoordinates.Builder()
        .setGroupId("com.example.group2")
        .setArtifactId("artifact2")
        .build();
    MavenCoordinates coordinates3 = new MavenCoordinates.Builder()
        .setGroupId("com.example.group3")
        .setArtifactId("artifact3")
        .build();
    List<LibraryFile> list2 = new ArrayList<>();
    LibraryFile file2 = new LibraryFile(coordinates2);
    LibraryFile file3 = new LibraryFile(coordinates3);
    list2.add(file2);
    list2.add(file3);
    
    Library library0 = new Library("id0");
    library0.setLibraryFiles(list0);
    Library library1 = new Library("id1");
    library1.setLibraryFiles(list1);
    Library library2 = new Library("id2");
    library2.setLibraryFiles(list2);
    List<Library> libraries = new ArrayList<>();
    libraries.add(library0);
    libraries.add(library1);
    libraries.add(library2);
    
    pom.addDependencies(libraries);
    
    Assert.assertEquals(1, pomFile.getHistory(null).length);
    
    InputStream contents = pomFile.getContents();
    Document actual = parse(contents);
    
    NodeList dependencies = actual.getElementsByTagName("dependencies");
    Assert.assertEquals(1, dependencies.getLength());
    NodeList children = ((Element) dependencies.item(0)).getElementsByTagName("dependency");
    
    Assert.assertEquals(4, children.getLength());
    
    Element child0 = (Element) children.item(0);
    Element groupId = getOnlyChild(child0, "groupId");
    Assert.assertEquals("com.example.group0", groupId.getTextContent());
    Element artifactId = getOnlyChild(child0, "artifactId");
    Assert.assertEquals("artifact0", artifactId.getTextContent());
    Element version = getOnlyChild(child0, "version");
    Assert.assertEquals("1.2.3", version.getTextContent());
    
    Element child3 = (Element) children.item(3);
    Element groupId3 = getOnlyChild(child3, "groupId");
    Assert.assertEquals("com.example.group3", groupId3.getTextContent());
    Element artifactId3 = getOnlyChild(child3, "artifactId");
    Assert.assertEquals("artifact3", artifactId3.getTextContent());
    
    // now make sure the comment didn't move to the end
    // https://bugs.openjdk.java.net/browse/JDK-8146163
    Assert.assertEquals(Node.COMMENT_NODE, actual.getChildNodes().item(0).getNodeType());
  }

  @Test
  public void testAddDependencies_withDuplicates() 
      throws CoreException, ParserConfigurationException, IOException, SAXException {

    MavenCoordinates coordinates1 = new MavenCoordinates.Builder()
        .setGroupId("com.example.group1")
        .setArtifactId("artifact1")
        .build();
    MavenCoordinates coordinates2 = new MavenCoordinates.Builder()
        .setGroupId("com.example.group2")
        .setArtifactId("artifact2")
        .build();

    List<LibraryFile> list1 = new ArrayList<>();
    LibraryFile file1 = new LibraryFile(coordinates1);
    list1.add(file1);
    
    List<LibraryFile> list2 = new ArrayList<>();
    LibraryFile file2 = new LibraryFile(coordinates2);
    list2.add(file1);
    list2.add(file2);
    
    Library library1 = new Library("id1");
    library1.setLibraryFiles(list1);
    Library library2 = new Library("id2");
    library2.setLibraryFiles(list2);

    List<Library> libraries = new ArrayList<>();
    libraries.add(library1);
    libraries.add(library2);
    
    pom.addDependencies(libraries);
    
    InputStream contents = pomFile.getContents();
    Document actual = parse(contents);
    
    NodeList dependencies = actual.getElementsByTagName("dependencies");
    NodeList children = ((Element) dependencies.item(0)).getElementsByTagName("dependency");
    
    Assert.assertEquals(2, children.getLength());
    
    Element child0 = (Element) children.item(0);
    Element groupId = getOnlyChild(child0, "groupId");
    Assert.assertEquals("com.example.group1", groupId.getTextContent());
    Element artifactId = getOnlyChild(child0, "artifactId");
    Assert.assertEquals("artifact1", artifactId.getTextContent());
    
    Element child1 = (Element) children.item(1);
    Element groupId1 = getOnlyChild(child1, "groupId");
    Assert.assertEquals("com.example.group2", groupId1.getTextContent());
    Element artifactId1 = getOnlyChild(child1, "artifactId");
    Assert.assertEquals("artifact2", artifactId1.getTextContent());
  }
  
  @Test
  public void testAddDependencies_areDirect() 
      throws CoreException, ParserConfigurationException, IOException, SAXException {
    
    // objectify depends on guava
    MavenCoordinates coordinates =
        new MavenCoordinates.Builder()
            .setGroupId("com.googlecode.objectify")
            .setArtifactId("objectify")
            .setVersion("5.1.10").build(); // to be upgraded
    
    List<LibraryFile> files = new ArrayList<>();
    LibraryFile file = new LibraryFile(coordinates);
    files.add(file);
    
    Library library = new Library("objectify");
    library.setLibraryFiles(files);
    
    List<Library> libraries = new ArrayList<>();
    libraries.add(library);
    
    pom.addDependencies(libraries);
    
    InputStream contents = pomFile.getContents();
    Document actual = parse(contents);
    
    NodeList dependencies = actual.getElementsByTagName("dependencies");
    Assert.assertEquals(1, dependencies.getLength());    
    
    Element dependency = getOnlyChild(((Element) dependencies.item(0)), "dependency");
    Element groupId = getOnlyChild(dependency, "groupId");
    Assert.assertEquals("com.googlecode.objectify", groupId.getTextContent());
    Element artifactId = getOnlyChild(dependency, "artifactId");
    Assert.assertEquals("objectify", artifactId.getTextContent());
    Element version = getOnlyChild(dependency, "version");
    Assert.assertNotEquals("5.1.10", version.getTextContent());
  }

  private static Document parse(InputStream in)
      throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document actual = builder.parse(in);
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
