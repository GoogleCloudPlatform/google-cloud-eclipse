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
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MavenUtils {

  /**
   * An operation that may modify the local Maven repository.
   *
   * @param <T> the return type of the operation
   */
  @FunctionalInterface
  interface MavenRepositoryOperation<T> {
    T run(IMavenExecutionContext context, RepositorySystem system, SubMonitor monitor)
        throws CoreException;
  }

  public static final String MAVEN2_NATURE_ID = "org.eclipse.m2e.core.maven2Nature"; //$NON-NLS-1$

  private static final Logger logger = Logger.getLogger(MavenUtils.class.getName());

  private static final String MAVEN_LATEST_VERSION = "LATEST"; //$NON-NLS-1$
  private static final String POM_XML_NAMESPACE_URI = "http://maven.apache.org/POM/4.0.0"; //$NON-NLS-1$

  /**
   * Returns {@code true} if the given project has the Maven 2 nature. This checks for the Maven
   * nature used by M2Eclipse 1.X.
   */
  public static boolean hasMavenNature(IProject project) {
    try {
      return NatureUtils.hasNature(project, MavenUtils.MAVEN2_NATURE_ID);
    } catch (CoreException coreException) {
      logger.log(Level.SEVERE, "Unable to examine natures on project " + project.getName(),
          coreException);
      return false;
    }
  }

  public static Artifact resolveArtifact(
      String groupId,
      String artifactId,
      String type,
      String version,
      String classifier,
      List<ArtifactRepository> repositories,
      IProgressMonitor monitor)
      throws CoreException {
    return runOperation(
        monitor,
        (context, system, progress) -> {
          IMaven maven = MavenPlugin.getMaven();
          Artifact artifact = maven.resolve(
              groupId, artifactId, version, type, classifier, repositories, progress);
          return artifact;
        });
  }

  /**
   * Perform some Maven-related action that may result in a change to the local Maven repositories,
   * ensuring that required {@link ISchedulingRule scheduling rules} are held.
   */
  public static <T> T runOperation(IProgressMonitor monitor, MavenRepositoryOperation<T> operation)
      throws CoreException {
    SubMonitor progress = SubMonitor.convert(monitor, 10);
    ISchedulingRule rule = mavenResolvingRule();
    boolean acquireRule = Job.getJobManager().currentRule() == null;
    if (acquireRule) {
      Job.getJobManager().beginRule(rule, progress.split(2));
    }
    try {
      Verify.verify(
          Job.getJobManager().currentRule().contains(rule),
          "require holding superset of rule: " + rule);
      IMavenExecutionContext context = MavenPlugin.getMaven().createExecutionContext();
      return context.execute(
          (context2, monitor2) -> {
            // todo we'd prefer not to depend on m2e here
            RepositorySystem system = MavenPluginActivator.getDefault().getRepositorySystem();
            return operation.run(context2, system, SubMonitor.convert(monitor2));
          },
          progress.split(8));
    } finally {
      if (acquireRule) {
        Job.getJobManager().endRule(rule);
      }
    }
  }

  /** Return the m2e scheduling rule used to serialize access to the Maven repository. */
  public static ISchedulingRule mavenResolvingRule() {
    return MavenPlugin.getProjectConfigurationManager().getRule();
  }

  public static String getProperty(InputStream pomXml, String propertyName) throws CoreException {
    return getTopLevelValue(parse(pomXml), "properties", propertyName); //$NON-NLS-1$
  }

  private static Document parse(InputStream pomXml) throws CoreException {
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      return documentBuilderFactory.newDocumentBuilder().parse(pomXml);
    } catch (IOException | SAXException | ParserConfigurationException exception) {
      throw new CoreException(
          StatusUtil.error(MavenUtils.class, "Cannot parse pom.xml", exception));
    }
  }

  private static String getTopLevelValue(Document doc, String parentTagName, String childTagName)
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
      throw new CoreException(StatusUtil.error(
          MavenUtils.class, "Missing pom.xml element: " + childTagName, exception));
    }
  }

  public static ArtifactRepository createRepository(String id, String url) throws CoreException {
    return MavenPlugin.getMaven().createArtifactRepository(id, url);
  }

  /**
   * Checks if an artifact is available in the local repository. The artifact <code>version</code>
   * must be a specific value, cannot be "LATEST".
   */
  public static boolean isArtifactAvailableLocally(String groupId, String artifactId,
                                                   String version, String type,
                                                   String classifier) {
    try {
      Preconditions.checkArgument(!MAVEN_LATEST_VERSION.equals(version));
      String artifactPath =
          MavenPlugin.getMaven().getLocalRepository()
              .pathOf(new DefaultArtifact(groupId, artifactId, version, null /* scope */, type,
                                          classifier, new DefaultArtifactHandler(type)));
      return new File(artifactPath).exists();
    } catch (CoreException ex) {
      logger.log(Level.SEVERE, "Could not lookup local repository", ex);
      return false;
    }
  }
}
