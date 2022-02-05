/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.cloud.tools.eclipse.util.ArtifactRetriever;
import com.google.cloud.tools.eclipse.util.MappedNamespaceContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class Bug3380Test {

  @Rule
  public final TestProjectCreator projectCreator = new TestProjectCreator();
  private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
  private static final XPathFactory xpathFactory = XPathFactory.newInstance();

  @BeforeClass
  public static void configureParser() {
    factory.setNamespaceAware(true);
  }

  private Pom pom;
  private IFile pomFile;
  private final XPath xpath = xpathFactory.newXPath();

  @Before
  public void setUp() throws SAXException, IOException, CoreException {
    xpath.setNamespaceContext(new MappedNamespaceContext("m", "http://maven.apache.org/POM/4.0.0"));

    IProject project = projectCreator.getProject();
    pomFile = project.getFile("pom.xml");
    try (InputStream in =
        Files.newInputStream(Paths.get("testdata/testBug3380.xml").toAbsolutePath())) {
      pomFile.create(in, IFile.FORCE, null);
      pom = Pom.parse(pomFile);
    }

    Logger logger = Logger.getLogger(ArtifactRetriever.class.getName());
    logger.setLevel(Level.OFF);
  }

  @After
  public void tearDown() {
    Logger logger = Logger.getLogger(ArtifactRetriever.class.getName());
    logger.setLevel(null);
  }

  @Test
  public void testBug3380() throws CoreException, XPathExpressionException {
    LibraryFile addedFile = new LibraryFile(coordinates("com.google.cloud", "google-cloud-asset"));
    Library addedLibrary = newLibrary("asset", addedFile);

    LibraryFile existingFile =
        new LibraryFile(coordinates("com.google.cloud", "google-cloud-automl"));
    Library existingLibrary = newLibrary("automl", existingFile);

    LibraryFile removedFile =
        new LibraryFile(coordinates("com.google.cloud", "google-cloud-bigquery"));
    Library removedLibrary = newLibrary("bigquery", removedFile);

    pom.updateDependencies(Arrays.asList(addedLibrary, existingLibrary),
        Arrays.asList(removedLibrary));

    Assert.assertTrue(pom.dependencyManaged("com.google.cloud", "google-cloud-asset"));
    Assert.assertTrue(pom.dependencyManaged("com.google.cloud", "google-cloud-automl"));

    Element bomElement = (Element) xpath.evaluate(
        "//m:dependencyManagement/m:dependencies/m:dependency[m:groupId='com.google.cloud'][m:artifactId='libraries-bom']",
        pom.document.getDocumentElement(), XPathConstants.NODE);
    Assert.assertNotNull(bomElement); // Make sure BOM element added

    Element dependencies = (Element) xpath.evaluate("./m:dependencies",
        pom.document.getDocumentElement(), XPathConstants.NODE);
    Assert.assertNotNull(dependencies);

    Element dependency1 =
        pom.findDependency(dependencies, "com.google.cloud", "google-cloud-asset");
    Assert.assertNotNull(dependency1);

    Node versionElement1 = Pom.findChildByName(dependency1, "version");
    Assert.assertNull(versionElement1); // new element

    Element dependency2 =
        pom.findDependency(dependencies, "com.google.cloud", "google-cloud-automl");
    Assert.assertNotNull(dependency2);

    Node versionElement2 = Pom.findChildByName(dependency2, "version");
    Assert.assertNull(versionElement2); // existing element
  }

  private static Library newLibrary(String libraryId, LibraryFile... libraryFiles) {
    Library library = new Library(libraryId);
    library.setLibraryFiles(Arrays.asList(libraryFiles));
    return library;
  }

  private static MavenCoordinates coordinates(String groupId, String artifactId) {
    return new MavenCoordinates.Builder().setGroupId(groupId).setArtifactId(artifactId).build();
  }
}
