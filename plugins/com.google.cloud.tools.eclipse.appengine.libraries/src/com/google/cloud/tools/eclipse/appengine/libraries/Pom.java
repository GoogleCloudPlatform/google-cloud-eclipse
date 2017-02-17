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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.appengine.libraries.model.MavenCoordinates;
import com.google.common.base.Preconditions;

class Pom {

  // todo we're doing enough of this we should import or write some utilities
  private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
  
  static {
    factory.setNamespaceAware(true);
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
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(pomFile.getContents());
      Pom pom = new Pom(document, pomFile);
      return pom;
    } catch (ParserConfigurationException ex) {
      IStatus status = new Status(IStatus.ERROR,
          "com.google.cloud.tools.eclipse.appengine.libraries", ex.getMessage(), ex);
      throw new CoreException(status);
    }
  }

  public void addDependencies(List<Library> libraries) throws CoreException {
    // TODO Auto-generated method stub
    
    NodeList dependenciesList = document.getElementsByTagName("dependencies");
    Element dependencies;
    if (dependenciesList.getLength() > 0) {
      dependencies = (Element) dependenciesList.item(0);
    } else {
      dependencies = document.createElement("dependencies");
    }
    
    //todo dedup
    for (Library library : libraries) {
      for (LibraryFile artifact : library.getLibraryFiles()) {
        Element dependency = document.createElement("dependency");
        MavenCoordinates coordinates = artifact.getMavenCoordinates();
        
        Element groupId = document.createElement("groupId");
        groupId.setTextContent(coordinates.getGroupId());
        Element artifactId = document.createElement("artifactId");
        artifactId.setTextContent(coordinates.getArtifactId());
        dependency.appendChild(groupId);
        dependency.appendChild(artifactId);
        
        dependencies.appendChild(dependency);
      }
    }
    
    if (dependencies.getParentNode() == null) {
      document.getDocumentElement().appendChild(dependencies);
    }
    
    writeDocument();   
  }

  private void writeDocument() throws CoreException {
    DOMImplementationLS domImplementation = (DOMImplementationLS) document.getImplementation();
    LSSerializer serializer = domImplementation.createLSSerializer();
    
    LSOutput lsOutput =  domImplementation.createLSOutput();
    lsOutput.setEncoding("UTF-8");
    Writer stringWriter = new StringWriter();
    lsOutput.setCharacterStream(stringWriter);
    serializer.write(document, lsOutput);  
    
    InputStream in = new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8));
    // todo: do we really want to force?
    // todo: monitor
    // todo: DOMException, LSExcepiton
    pomFile.setContents(in, IFile.FORCE, null);
  }

}
