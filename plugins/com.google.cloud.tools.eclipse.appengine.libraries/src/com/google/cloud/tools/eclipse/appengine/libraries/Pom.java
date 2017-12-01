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
import com.google.cloud.tools.eclipse.util.ArtifactRetriever;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Verify;
import com.google.common.collect.Iterables;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class Pom {

  // todo we're doing enough of this we should import or write some utilities
  private static final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
  private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
  
  static {
    builderFactory.setNamespaceAware(true);
  }
  
  private Document document;
  private IFile pomFile;
  
  private Pom(Document document, IFile pomFile) {
    this.document = document;
    this.pomFile = pomFile;
  }

  static Pom parse(IFile pomFile) throws SAXException, IOException, CoreException {
    Preconditions.checkState(pomFile.exists(), pomFile.getFullPath() + " does not exist");
    
    try {
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      Document document = builder.parse(pomFile.getContents());
      Pom pom = new Pom(document, pomFile);
      return pom;
    } catch (ParserConfigurationException ex) {
      IStatus status = StatusUtil.error(Pom.class, ex.getMessage(), ex);
      throw new CoreException(status);
    }
  }

  /**
   * Select libraries whose artifacts are satisfied by the pom's dependencies.
   */
  public Collection<Library> resolveLibraries(Collection<Library> availableLibraries) {
    NodeList dependenciesList = document.getElementsByTagName("dependencies");
    if (dependenciesList.getLength() == 0) {
      return Collections.emptyList();
    }
    final Element dependencies = (Element) dependenciesList.item(0);

    Predicate<LibraryFile> dependencyFound = new Predicate<LibraryFile>() {
      @Override
      public boolean apply(LibraryFile libraryFile) {
        Preconditions.checkNotNull(libraryFile);
        MavenCoordinates coordinates = libraryFile.getMavenCoordinates();
        String groupId = coordinates.getGroupId();
        String artifactId = coordinates.getArtifactId();
        return dependencyExists(dependencies, groupId, artifactId);
      }
    };

    List<Library> matched = new ArrayList<>();
    for(Library library : availableLibraries) {
      if (Iterables.all(library.getDirectDependencies(), dependencyFound)) {
        matched.add(library);
      }
    }
    return matched;
  }

  /** Add dependencies required for the list of selected libraries. */
  void addDependencies(List<Library> selected) throws CoreException {
    addDependencies(selected, null);
  }

  /**
   * Adjust dependencies required for the list of selected libraries, adding any necessary
   * dependencies and removing any unnecessary dependencies.
   * 
   * @param selected the set of libraries to be included
   * @param availableLibraries if not {@code null}, then remove any dependencies that match
   *        unselected libraries in this set (i.e., they were unselected). If {@code null} then
   *        libraries are never removed.
   */
  void addDependencies(List<Library> selected, Collection<Library> availableLibraries)
      throws CoreException {
    // see
    // m2e-core/org.eclipse.m2e.core.ui/src/org/eclipse/m2e/core/ui/internal/actions/AddDependencyAction.java
    // m2e-core/org.eclipse.m2e.core.ui/src/org/eclipse/m2e/core/ui/internal/editing/AddDependencyOperation.java
    
    NodeList dependenciesList = document.getElementsByTagName("dependencies");
    Element dependencies;
    if (dependenciesList.getLength() > 0) {
      dependencies = (Element) dependenciesList.item(0);
    } else {
      dependencies = document.createElement("dependencies");
    }

    if (availableLibraries != null) {
      removeUnusedDependencies(dependencies, selected, availableLibraries);
    }

    for (Library library : selected) {
      for (LibraryFile artifact : library.getDirectDependencies()) {
        MavenCoordinates coordinates = artifact.getMavenCoordinates();
        
        String groupId = coordinates.getGroupId();
        String artifactId = coordinates.getArtifactId();
        
        if (!dependencyExists(dependencies, groupId, artifactId)) {
          Element dependency = document.createElement("dependency");
          Element groupIdElement = document.createElement("groupId");
          groupIdElement.setTextContent(groupId);
          dependency.appendChild(groupIdElement);

          Element artifactIdElement = document.createElement("artifactId");
          artifactIdElement.setTextContent(artifactId);
          dependency.appendChild(artifactIdElement);

          String version = coordinates.getVersion();
          ArtifactVersion latestVersion =
              ArtifactRetriever.DEFAULT.getBestVersion(groupId, artifactId);
          if (latestVersion != null) {
            version = latestVersion.toString(); 
          }
          
          // todo latest version may not be needed anymore.
          if (!MavenCoordinates.LATEST_VERSION.equals(version)) {
            Element versionElement = document.createElement("version");
            versionElement.setTextContent(version);
            dependency.appendChild(versionElement);
          }
          
          dependencies.appendChild(dependency);
        }
      }
    }
    
    if (dependencies.getParentNode() == null) {
      document.getDocumentElement().appendChild(dependencies);
    }
    
    try {
      writeDocument();
    } catch (TransformerException ex) {
      throw new CoreException(null);
    }   
  }

  /**
   * Remove any dependencies that were required by the previously-selected libraries and that are
   * not required by the currently selected libraries.
   * 
   * @param selectedLibraries the currently selected libraries
   * @param previousLibraries the previously-selected libraries
   */
  @VisibleForTesting
  static void removeUnusedDependencies(Element dependencies,
      Collection<Library> selectedLibraries,
      Collection<Library> previousLibraries) {

    Set<String> selectedDependencies = new HashSet<>();
    for (Library library : selectedLibraries) {
      for (LibraryFile libraryFile : library.getDirectDependencies()) {
        MavenCoordinates coordinates = libraryFile.getMavenCoordinates();
        String encoded = coordinates.getGroupId() + ":" + coordinates.getArtifactId(); //$NON-NLS-1$
        selectedDependencies.add(encoded);
      }
    }
    Verify.verify(selectedDependencies.isEmpty() == selectedLibraries.isEmpty());

    Map<String, Node> currentDependencies = new HashMap<>();
    NodeList children = dependencies.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element dependency = (Element) node;
        String groupId = getValue(dependency, "groupId"); //$NON-NLS-1$
        String artifactId = getValue(dependency, "artifactId"); //$NON-NLS-1$
        String encoded = groupId + ":" + artifactId; //$NON-NLS-1$
        currentDependencies.put(encoded, node);
      }
    }
    Verify.verify(currentDependencies.isEmpty() == (dependencies.getChildNodes().getLength() == 0));

    Set<Node> nodesToRemove = new HashSet<>();
    for (Library library : previousLibraries) {
      // true if all coordinates for this library are found
      boolean allFound = true;
      Set<Node> libraryNodes = new HashSet<>();
      for (LibraryFile file : library.getDirectDependencies()) {
        MavenCoordinates coord = file.getMavenCoordinates();
        String encoded = coord.getGroupId() + ":" + coord.getArtifactId(); //$NON-NLS-1$
        allFound &= currentDependencies.containsKey(encoded);
        // if not required by selected libraries then mark for removal
        if (allFound && !selectedDependencies.contains(encoded)) {
          libraryNodes.add(currentDependencies.get(encoded));
        }
      }
      // previous library definition was satisfied
      if (allFound) {
        nodesToRemove.addAll(libraryNodes);
      }
    }

    for (Node node : nodesToRemove) {
      dependencies.removeChild(node);
    }
  }

  private boolean dependencyExists(Element dependencies, String targetGroupId,
      String targetArtifactId) {
    
    NodeList children = dependencies.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element dependency = (Element) node;
        String groupId = getValue(dependency, "groupId");
        String artifactId = getValue(dependency, "artifactId");
        if (targetGroupId.equals(groupId) && targetArtifactId.equals(artifactId)) {
          return true;
        }
      }
    }
    return false;
  }

  private static String getValue(Element dependency, String childName) {
    NodeList children = dependency.getElementsByTagName(childName);
    if (children.getLength() > 0) {
      return children.item(0).getTextContent();
    }
    return null;
  }

  private void writeDocument() throws CoreException, TransformerException {
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    transformer.transform(new DOMSource(document), new StreamResult(out));
    InputStream in = new ByteArrayInputStream(out.toByteArray());
    
    pomFile.setContents(in, true, true, null);
  }
}
