/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.project;

import com.google.cloud.tools.eclipse.dataflow.core.DataflowCorePlugin;
import com.google.cloud.tools.eclipse.dataflow.core.natures.DataflowJavaProjectNature;
import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.IOException;
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * DataflowMavenModel provides methods to mutate a Maven POM in-place.
 */
public class DataflowMavenModel {
  private final XPath xpath;

  private final DataflowDependencyManager dependencyManager;
  private final IProject project;
  private final IDOMModel model;

  @VisibleForTesting
  DataflowMavenModel(
      DataflowDependencyManager dependencyManager, XPath xpath, IProject project, IDOMModel model) {
    this.dependencyManager = dependencyManager;
    this.xpath = xpath;
    this.project = project;
    this.model = model;
  }

  /**
   * Interface describing an edit that can be applied to a {@code pom.xml}.
   */
  private interface PomEdit {
    /**
     * Returns the XPath pattern to use for selecting nodes to edit.
     */
    String getXpathPattern();

    /**
     * This we be called on each node that matches the XPath.
     *
     * <p>It should apply the edit, if appropriate.
     */
    void apply(Node matchingNode);
  }

  /**
   * An XPath expression that retrieves the version tag of the Dataflow Dependency in a POM.
   *
   * <p>Specifies the version element that is a child of a dependency element containing an
   * artifactId element with contents equal to the SDK artifact ID, which is a child of the
   * dependencies element, which is a child of the root level project element.
   */
  @VisibleForTesting
  static final String DATAFLOW_VERSION_XPATH_EXPR = String.format(
      "/pom:project/pom:dependencies/pom:dependency[pom:artifactId='%s']/pom:version",
      DataflowArtifactRetriever.DATAFLOW_SDK_ARTIFACT);

  private static class SetDataflowDependencyVersion implements PomEdit {
    private final VersionRange version;

    public SetDataflowDependencyVersion(VersionRange version) {
      this.version = version;
    }

    @Override
    public String getXpathPattern() {
      return DATAFLOW_VERSION_XPATH_EXPR;
    }

    @Override
    public void apply(Node matchingNode) {
      matchingNode.getFirstChild().setNodeValue(version.toString());
    }
  }

  private void editPom(PomEdit... editOperations) throws IOException, CoreException {
      model.aboutToChangeModel();
      model.getUndoManager().beginRecording(model);
      for (PomEdit editOperation : editOperations) {
        NodeList exprResults = getMatchingNodes(editOperation.getXpathPattern());
        for (int i = 0; i < exprResults.getLength(); i++) {
          Node node = exprResults.item(i);
          editOperation.apply(node);
        }
      }
      model.getUndoManager().endRecording(model);
      model.changedModel();
      model.save();
  }

  private NodeList getMatchingNodes(String xpathPattern) {
    try {
      return getMatchingNodes(xpath, model.getDocument(), xpathPattern);
    } catch (XPathExpressionException e) {
      throw new IllegalArgumentException("XPath pattern '" + xpathPattern + "' was malformed", e);
    }
  }

  @VisibleForTesting
  static NodeList getMatchingNodes(XPath xpath, Document document, String xpathPattern)
      throws XPathExpressionException {
    XPathExpression expr = xpath.compile(xpathPattern);
    return (NodeList) expr.evaluate(document, XPathConstants.NODESET);
  }

  /**
   * Sets the Dataflow Dependency of this model to the [current latest version, next major version).
   */
  public void pinDataflowDependencyToCurrent(IProgressMonitor monitor) throws CoreException {
    try {
      VersionRange existingVersionRange = dependencyManager.getDataflowVersionRange(project);
      ArtifactVersion version =
          dependencyManager.getLatestDataflowDependencyInRange(existingVersionRange);
      VersionRange newRange = MajorVersion.truncateAtLatest(version, existingVersionRange);
      editPom(new SetDataflowDependencyVersion(newRange));
    } catch (IOException e) {
      throw new CoreException(
          new Status(
              Status.ERROR,
              DataflowCorePlugin.PLUGIN_ID,
              "Exception when trying to pin Dataflow Dependency",
              e));
    } finally {
      monitor.done();
    }
  }

  /**
   * Sets the Dataflow Dependency of this model to the LATEST version value.
   */
  @Deprecated
  public void trackDataflowDependency(@SuppressWarnings("unused") IProgressMonitor monitor)
      throws CoreException {
    try {
      editPom(
          new SetDataflowDependencyVersion(
              VersionRange.createFromVersion(Artifact.LATEST_VERSION)));
    } catch (IOException e) {
      throw new CoreException(new Status(Status.ERROR, DataflowCorePlugin.PLUGIN_ID,
          "Exception when trying to track Dataflow Dependency", e));
    }
  }

  /**
   * A factory to create {@link DataflowMavenModel DataflowMavenModels}. A DataflowMavenModel is a
   * view of the XML representation of a Maven model (i.e. the pom.xml), and can be used to modify
   * the POM in-place.
   */
  public static class DataflowMavenModelFactory {
    /**
     * A namespace context that provides the namespace for a pom file.
     *
     * <p>A well-formed POM file declares a namespace in the project element. This NamespaceContext
     * provides that namespace for the prefix 'pom'.
     */
    @VisibleForTesting
    static final NamespaceContext POM_NS_CONTEXT = new NamespaceContext() {
      @Override
      public String getNamespaceURI(String prefix) {
        if (prefix == null) {
          throw new NullPointerException("Null prefix");
        } else if ("pom".equals(prefix)) {
          return "http://maven.apache.org/POM/4.0.0";
        } else if ("xml".equals(prefix)) {
          return XMLConstants.XML_NS_URI;
        }
        return XMLConstants.NULL_NS_URI;
      }

      @Override
      public String getPrefix(String namespaceURI) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Iterator<?> getPrefixes(String namespaceURI) {
        throw new UnsupportedOperationException();
      }
    };

    private final DataflowDependencyManager dependencyManager;
    private final IMavenProjectRegistry projectRegistry;

    public DataflowMavenModelFactory() {
      this(DataflowDependencyManager.create(), MavenPlugin.getMavenProjectRegistry());
    }

    public DataflowMavenModelFactory(
        DataflowDependencyManager dependencyManager, IMavenProjectRegistry projectRegistry) {
      this.dependencyManager = dependencyManager;
      this.projectRegistry = projectRegistry;
    }

    public DataflowMavenModel fromProject(IProject project) throws CoreException {
      if (!DataflowJavaProjectNature.hasDataflowNature(project)) {
        String errorMessage = String.format(
            "Tried to create the Dataflow dependency of a non-Dataflow project %s",
            project.getName());
        DataflowCorePlugin.logWarning(errorMessage);
        throw new IllegalArgumentException(errorMessage);
      }
      IMavenProjectFacade facade = projectRegistry.getProject(project);
      IFile pomFile = facade.getPom();
      return fromIFile(pomFile);
    }

    private DataflowMavenModel fromIFile(IFile file) throws CoreException {
      try {
        IStructuredModel structuredModel =
            StructuredModelManager.getModelManager().getModelForEdit(file);
        if (structuredModel instanceof IDOMModel) {
          XPath xpath = XPathFactory.newInstance().newXPath();
          xpath.setNamespaceContext(POM_NS_CONTEXT);
          return new DataflowMavenModel(
              dependencyManager, xpath, file.getProject(), (IDOMModel) structuredModel);
        } else {
          throw new CoreException(new Status(Status.ERROR, DataflowCorePlugin.PLUGIN_ID,
              String.format("File %s wasn't a DOM model", file)));
        }
      } catch (IOException e) {
        throw new CoreException(new Status(Status.ERROR, DataflowCorePlugin.PLUGIN_ID,
            String.format("Couldn't get a DOM Model for file %s", file), e));
      }
    }
  }
}
