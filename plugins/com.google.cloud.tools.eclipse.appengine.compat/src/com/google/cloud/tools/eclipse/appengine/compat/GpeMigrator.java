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

package com.google.cloud.tools.eclipse.appengine.compat;

import com.google.cloud.tools.eclipse.util.NatureUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.internal.FacetedProject;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GpeMigrator {

  private static final Logger logger = Logger.getLogger(GpeMigrator.class.getName());

  private static final String GPE_GAE_NATURE_ID = "com.google.appengine.eclipse.core.gaeNature";

  private static final String GPE_WTP_GAE_FACET_ID = "com.google.appengine.facet";
  private static final String GPE_WTP_GAE_EAR_FACET_ID = "com.google.appengine.facet.ear";

  private static final String GPE_WTP_GAE_RUNTIME = "com.google.appengine.runtime.id";

  private static final ImmutableList<String> GPE_CLASSPATH_ENTRIES_PATH = ImmutableList.of(
      "org.eclipse.jst.server.core.container/com.google.appengine.server.runtimeTarget/Google App Engine",
      "com.google.appengine.eclipse.core.GAE_CONTAINER",
      "com.google.appengine.eclipse.wtp.GAE_WTP_CONTAINER"
  );

  // XML element and attribute name used to save installed facets in WTP facet settings XML file.
  private static final String ELEMENT_NAME_INSTALLED_FACET = "installed";
  private static final String ATTRIBUTE_NAME_FACET_ID = "facet";

  public static void removeObsoleteGpeFixtures(
      final IFacetedProject facetedProject, IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, 40);

    // 1. Remove classpath entries from GPE.
    IJavaProject javaProject = JavaCore.create(facetedProject.getProject());
    List<IClasspathEntry> newEntries = new ArrayList<>();
    for (IClasspathEntry entry : javaProject.getRawClasspath()) {
      String path = entry.getPath().toString();  // note: '/' is a path separator.
      if (!GPE_CLASSPATH_ENTRIES_PATH.contains(path)) {
        newEntries.add(entry);
      }
    }

    IClasspathEntry[] rawEntries = newEntries.toArray(new IClasspathEntry[0]);
    javaProject.setRawClasspath(rawEntries, subMonitor.newChild(10));
    javaProject.save(new NullProgressMonitor(), true);

    // 2. Remove JARs under "WEB-INF/lib" added by GPE.


    // 3. Remove GPE nature.
    NatureUtils.removeNature(facetedProject.getProject(), GPE_GAE_NATURE_ID);
    subMonitor.worked(10);

    // 4. Remove GPE runtime.
    Set<IRuntime> runtimes = facetedProject.getTargetedRuntimes();
    for (IRuntime runtime : runtimes) {
      if (GPE_WTP_GAE_RUNTIME.equals(runtime.getProperty("id"))) {
        facetedProject.removeTargetedRuntime(runtime, null /* monitor */);
      }
    }
    subMonitor.worked(10);

    // 5. Remove GPE facets.
    IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        removeGpeFacets(facetedProject);
      }
    };
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.run(runnable, workspace.getRoot(), IWorkspace.AVOID_UPDATE, null);
    subMonitor.worked(10);
  }

  @VisibleForTesting
  static void removeGpeFacets(IFacetedProject facetedProject) {
    try {
      // To remove the facets, we will directly modify the WTP facet metadata file:
      // .settings/org.eclipse.wst.common.project.facet.core.xml
      IFile metadataFile = facetedProject.getProject().getFile(FacetedProject.METADATA_FILE);
      try (InputStream stream = metadataFile.getContents()) {

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Element root = builder.parse(stream).getDocumentElement();

        List<Node> gpeFacetNodes = collectGpeFacetNodes(root);
        for (Node gpeNode : gpeFacetNodes) {
          root.removeChild(gpeNode);
        }

        saveDomToFile(root.getOwnerDocument(), metadataFile);
      }

    } catch (ParserConfigurationException | SAXException
        | IOException | TransformerException | CoreException ex) {
      logger.log(Level.WARNING, "Cannot modify WTP facet metadata file to remove GEP facets.", ex);
    }
  }

  @VisibleForTesting
  static List<Node> collectGpeFacetNodes(Element root) {
    List<Node> gpeFacetNodes = new ArrayList<>();

    NodeList nodeList = root.getChildNodes();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);

      if (node.getNodeType() == Node.ELEMENT_NODE
          && ELEMENT_NAME_INSTALLED_FACET.equals(node.getNodeName())) {
        String facetId = ((Element) node).getAttribute(ATTRIBUTE_NAME_FACET_ID);
        if (GPE_WTP_GAE_FACET_ID.equals(facetId) || GPE_WTP_GAE_EAR_FACET_ID.equals(facetId)) {
          gpeFacetNodes.add(node);
        }
      }
    }
    return gpeFacetNodes;
  }

  @VisibleForTesting
  static void saveDomToFile(Document document, IFile file)
      throws IOException, TransformerException, CoreException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.transform(new DOMSource(document), new StreamResult(outputStream));
      InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

      file.setContents(inputStream, IFile.FORCE, null /* monitor */);
    }
  }
}
