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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowMavenModel.DataflowMavenModelFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.sse.core.internal.undo.IStructuredTextUndoManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

/**
 * Tests for {@link DataflowMavenModel}.
 */
public class DataflowMavenModelTest {
  private DataflowMavenModel model;

  @Mock private DataflowDependencyManager dependencyManager;
  @Mock IProject project;

  @Mock private IDOMModel domModel;
  @Mock private IDOMDocument document;
  @Mock private XPath xpath;
  @Mock private XPathExpression xpathExpression;
  @Mock private Artifact artifact;
  @Mock private IStructuredTextUndoManager undoManager;

  private IProgressMonitor monitor;
  private ArtifactVersion latestVersion;
  private VersionRange currentVersionSpec;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    latestVersion = new DefaultArtifactVersion("1.20.0-beta1");
    currentVersionSpec = VersionRange.createFromVersionSpec("[1.2.3, 1.99.0)");
    when(dependencyManager.getDataflowVersionRange(project)).thenReturn(currentVersionSpec);
    when(dependencyManager.getLatestDataflowDependencyInRange(currentVersionSpec))
        .thenReturn(latestVersion);
    model = new DataflowMavenModel(dependencyManager, xpath, project, domModel);

    when(domModel.getDocument()).thenReturn(document);
    when(domModel.getUndoManager()).thenReturn(undoManager);

    monitor = new NullProgressMonitor();
  }

  @Test
  public void testTrackDataflowDependency() throws Exception {
    when(xpath.compile(DataflowMavenModel.DATAFLOW_VERSION_XPATH_EXPR)).thenReturn(xpathExpression);

    NodeList versionNodeList = mock(NodeList.class);
    Node versionNode = mock(Node.class);
    when(versionNodeList.getLength()).thenReturn(1);
    when(versionNodeList.item(0)).thenReturn(versionNode);

    Node versionNodeText = mock(Node.class);
    when(versionNode.getFirstChild()).thenReturn(versionNodeText);

    when(xpathExpression.evaluate(document, XPathConstants.NODESET)).thenReturn(versionNodeList);

    model.trackDataflowDependency(monitor);

    verify(domModel).aboutToChangeModel();
    verify(undoManager).beginRecording(domModel);
    verify(versionNodeText).setNodeValue(Artifact.LATEST_VERSION);
    verify(undoManager).endRecording(domModel);
    verify(domModel).changedModel();
    verify(domModel).save();
  }

  @Test
  public void testPinDataflowDependencyWithDynamicVersion() throws Exception {
    when(xpath.compile(DataflowMavenModel.DATAFLOW_VERSION_XPATH_EXPR)).thenReturn(xpathExpression);

    NodeList versionNodeList = mock(NodeList.class);
    Node versionNode = mock(Node.class);
    when(versionNodeList.getLength()).thenReturn(1);
    when(versionNodeList.item(0)).thenReturn(versionNode);

    Node versionNodeText = mock(Node.class);
    when(versionNode.getFirstChild()).thenReturn(versionNodeText);

    when(xpathExpression.evaluate(document, XPathConstants.NODESET)).thenReturn(versionNodeList);

    model.pinDataflowDependencyToCurrent(monitor);

    verify(domModel).aboutToChangeModel();
    verify(undoManager).beginRecording(domModel);
    verify(versionNodeText)
        .setNodeValue(
            String.format("[%s,%s)", latestVersion.toString(), MajorVersion.ONE.getMaxVersion()));
    verify(undoManager).endRecording(domModel);
    verify(domModel).changedModel();
    verify(domModel).save();
  }

  @Test
  public void testXpathExpression() throws Exception {
    String simplifiedModel =
        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" "
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        + "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 "
        + "http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
        + "  <dependencies>\n"
        + "    <dependency>\n"
        + "      <groupId>com.google.cloud.dataflow</groupId>\n"
        + "      <artifactId>google-cloud-dataflow-java-sdk-all</artifactId>\n"
        + "      <version>FOO-BAR</version>\n"
        + "    </dependency>\n"
        + "    <dependency>\n"
        + "      <groupId>com.google.apis</groupId>\n"
        + "      <artifactId>google-api-services-storage</artifactId>\n"
        + "      <version>v1-rev25-1.19.1</version>\n"
        + "      <exclusions>\n"
        + "        <exclusion>\n"
        + "          <!-- What is this nonsense -->        \n"
        + "          <artifactId>guava-jdk5</artifactId>\n"
        + "          <groupId>com.google.guava</groupId>\n"
        + "        </exclusion>\n"
        + "      </exclusions>\n"
        + "    </dependency>\n"
        + "  </dependencies>\n"
        + "  <modelVersion>4.0.0</modelVersion>\n"
        + "  <groupId>com.example.group.id</groupId>\n"
        + "  <artifactId>YourProj</artifactId>\n"
        + "  <version>0.0.1-SNAPSHOT</version>\n"
        + "</project>";

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document simpleModelDocument = dbf.newDocumentBuilder().parse(
        new ByteArrayInputStream(simplifiedModel.getBytes()));

    XPath realXpath = XPathFactory.newInstance().newXPath();
    realXpath.setNamespaceContext(DataflowMavenModelFactory.POM_NS_CONTEXT);
    NodeList matchingNodes =
        DataflowMavenModel.getMatchingNodes(realXpath,
            simpleModelDocument, DataflowMavenModel.DATAFLOW_VERSION_XPATH_EXPR);

    assertEquals(1, matchingNodes.getLength());
    Node matched = matchingNodes.item(0);
    assertEquals("version", matched.getLocalName());
    assertEquals("FOO-BAR", matched.getTextContent());
  }
}
