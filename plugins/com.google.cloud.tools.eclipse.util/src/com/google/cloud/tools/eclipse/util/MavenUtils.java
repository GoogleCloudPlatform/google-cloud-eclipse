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

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.base.Objects;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.MavenPlugin;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MavenUtils {

  public static final String MAVEN2_NATURE_ID = "org.eclipse.m2e.core.maven2Nature";

  private static final Logger logger = Logger.getLogger(MavenUtils.class.getName());

  private static final String POM_XML_NAMESPACE_URI = "http://maven.apache.org/POM/4.0.0";
  
  /**
   * Returns {@code true} if the given project has the Maven 2 nature. This
   * checks for the Maven nature used by m2Eclipse 1.0.0.
   */
  public static boolean hasMavenNature(IProject project) {
    try {
      if (NatureUtils.hasNature(project, MavenUtils.MAVEN2_NATURE_ID)) {
        return true;
      }
    } catch (CoreException coreException) {
      logger.log(Level.SEVERE, "Unable to examine natures on project " + project.getName(), coreException);
    }
    return false;
  }

  /**
   * Returns true if the group IDs and artifact IDs of <code>dependency1</code> and
   * <@code>dependency2</@code> are equal. Returns false otherwise.
   */
  public static boolean areDependenciesEqual(Dependency dependency1, Dependency dependency2) {
    if ((dependency1 == null) || (dependency2 == null)) {
      return false;
    }

    if (!Objects.equal(dependency1.getGroupId(), dependency2.getGroupId())||
        !Objects.equal(dependency1.getArtifactId(), dependency2.getArtifactId())) {
      return false;
    }

    return true;
  }

  /**
   * Returns true if a dependency with the same group ID and artifact ID as <code>targetDependency</code>
   * exists in <code>dependencies</code>. Returns false otherwise.
   */
  public static boolean doesListContainDependency(List<Dependency> dependencies, Dependency targetDependency) {
    if((dependencies == null) || (targetDependency == null)) {
      return false;
    }

    for (Dependency dependency : dependencies) {
      if (areDependenciesEqual(dependency, targetDependency)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns the latest version of the maven artifact specified via <code>groupId</code> and
   * <code>artifactId</code> or the <code>defaultVersion</code> if an error occurs while fetching
   * the latest version.
   */
  public static String resolveLatestReleasedArtifactVersion(IProgressMonitor monitor, String groupId,
                                                            String artifactId, String type, String defaultVersion) {
    try {
      Artifact artifact = resolveArtifact(monitor, groupId, artifactId, type, "LATEST");
      return artifact.getVersion();
    } catch (CoreException ex) {
      logger.log(Level.WARNING,
                 MessageFormat.format("Unable to resolve artifact {0}:{1}", groupId, artifactId), ex);
      return defaultVersion;
    }
  }

  public static Artifact resolveArtifact(IProgressMonitor monitor, String groupId, String artifactId,
                                         String type, String version) throws CoreException {
    return resolveArtifact(monitor, groupId, artifactId, type, version, null, null);
  }

  public static Artifact resolveArtifact(IProgressMonitor monitor, String groupId, String artifactId,
                                         String type, String version, String classifier,
                                         List<ArtifactRepository> repositories) throws CoreException {
    Artifact artifact = MavenPlugin.getMaven().resolve(groupId, artifactId, version, type,
                                                       classifier, repositories, monitor);
    return artifact;
  }

  public String getProperty(InputStream pomXml, String propertyName) throws CoreException {
    return getTopLevelValue(parse(pomXml), "properties", propertyName); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private Document parse(InputStream pomXml) throws CoreException {
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      return documentBuilderFactory.newDocumentBuilder().parse(pomXml);
    } catch (IOException | SAXException | ParserConfigurationException exception) {
      throw new CoreException(StatusUtil.error(getClass(), "Cannot parse pom.xml", exception));
    }
  }

  private String getTopLevelValue(Document doc, String parentTagName, String childTagName)
      throws CoreException {
    try {
      NodeList parentElements = doc.getElementsByTagNameNS(POM_XML_NAMESPACE_URI, parentTagName);
      if (parentElements.getLength() > 0) {
        Node parent = parentElements.item(0);
        if (parent.hasChildNodes()) {
          for (int i = 0; i < parent.getChildNodes().getLength(); ++i) {
            Node child = parent.getChildNodes().item(i);
            if (child.getNodeName().equals(childTagName) && (child.getNamespaceURI() == null
                || POM_XML_NAMESPACE_URI.equals(child.getNamespaceURI()))) {
              return child.getTextContent();
            }
          }
        }
      }
      return null;
    } catch (DOMException exception) {
      throw new CoreException(
          new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass()).getSymbolicName(),
              "Missing pom.xml element: " + childTagName, exception));
    }
  }

  public static ArtifactRepository createRepository(String id, String url) throws CoreException {
    return MavenPlugin.getMaven().createArtifactRepository(id, url);
  }

}
