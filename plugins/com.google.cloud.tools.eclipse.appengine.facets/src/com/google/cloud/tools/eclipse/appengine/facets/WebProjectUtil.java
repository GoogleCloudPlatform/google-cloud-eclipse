/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.facets;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.WorkbenchComponent;
import org.eclipse.wst.common.componentcore.resources.ITaggedVirtualResource;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Utility classes for processing WTP Web Projects (jst.web and jst.utility).
 */
public class WebProjectUtil {
  // This is the folder created by default when the Dynamic Web Module facet is installed
  // (unless the user changes the default.)
  public static final Path DEFAULT_DYNAMIC_WEB_FACET_WEB_CONTENT_PATH = new Path("/WebContent");

  private static final String DEFAULT_WEB_PATH = "src/main/webapp";

  private static final String WEB_INF = "WEB-INF/";

  /**
   * Return the project's <code>WEB-INF</code> directory. There is no guarantee that the contents
   * are actually published.
   *
   * @return the <code>IFolder</code> or null if not present
   */
  public static IFolder getWebInfDirectory(IProject project) {
    // Try to obtain the directory as if it was a Dynamic Web Project
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component != null && component.exists()) {
      IVirtualFolder root = component.getRootFolder();
      // the root should exist, but the WEB-INF may not yet exist
      if (root.exists()) {
        return (IFolder) root.getFolder(WEB_INF).getUnderlyingFolder();
      }
    }
    // Otherwise it's seemingly fair game
    IFolder defaultLocation = project.getFolder(DEFAULT_WEB_PATH).getFolder(WEB_INF);
    if (defaultLocation.exists()) {
      return defaultLocation;
    }
    return null;
  }

  /**
   * Attempt to resolve the given file within the project's <code>WEB-INF</code>.
   *
   * @return the file location or {@code null} if not found
   */
  public static IFile findInWebInf(IProject project, IPath filePath) {
    IFolder webInfFolder = getWebInfDirectory(project);
    if (webInfFolder == null) {
      return null;
    }
    IFile file = webInfFolder.getFile(filePath);
    return file.exists() ? file : null;
  }

  private static List<IFolder> findAllWebInfFolders(IContainer container) {
    List<IFolder> webInfFolders = new ArrayList<>();
    try {
      for (IResource resource : container.members()) {
        if (resource.exists() && resource.getType() == IResource.FOLDER) {
          if ("WEB-INF".equals(resource.getName())) {
            webInfFolders.add((IFolder) resource);
          } else {
            webInfFolders.addAll(findAllWebInfFolders((IFolder) resource));
          }
        }
      }
    } catch (CoreException ex) {}
    return webInfFolders;
  }

  private static boolean hasMultipleWebInfFolder(IProject project) {
    return findAllWebInfFolders(project).size() > 1;
  }

  /**
   * Returns the web content path setting designated by the Dynamic Web Module facet. May
   * return {@code null}.
   *
   * The path information resides in an XML file, which a project settings file:
   * ".settings/org.eclipse.wst.common.component". This methods effectively returns the path
   * tagged with {@code "defaultRootSource"} in the XML file.
   */
  private static IPath getDefaultWebRootFolder(IProject project) {
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component != null && component.exists()) {
      IVirtualFolder rootFolder = component.getRootFolder();
      if (rootFolder instanceof ITaggedVirtualResource) {
        ITaggedVirtualResource resource = (ITaggedVirtualResource) rootFolder;
        IPath defaultRootSource = resource.getFirstTaggedResource(
            WorkbenchComponent.DEFAULT_ROOT_SOURCE_TAG /* "defaultRootSource" String literal */);
        return defaultRootSource;
      }
    }
    return null;
  }

  public static boolean hasBogusWebRootFolder(IProject project) {
    IPath webContentPath = getDefaultWebRootFolder(project);
    if (!DEFAULT_DYNAMIC_WEB_FACET_WEB_CONTENT_PATH.equals(webContentPath)) {
      return false;
    }

    IFolder webContentFolder = project.getFolder(webContentPath);
    if (!webContentFolder.exists()) {  // Also covers checking if it's actually a folder.
      return false;
    }

    if (isWtpGeneratedWebContentRoot(webContentFolder) && hasMultipleWebInfFolder(project)) {
      return true;
    }
    return false;
  }

  private static boolean isWtpGeneratedWebContentRoot(IFolder webContentRoot) {
    try {
      if (webContentRoot.members().length != 2) {  // WEB-INF and META-INF should be the only two.
        return false;
      }
      IResource metaInf = webContentRoot.findMember("/META-INF");
      IResource webInf = webContentRoot.findMember("/WEB-INF");
      if (metaInf == null || webInf == null
          || metaInf.getType() != IResource.FOLDER || webInf.getType() != IResource.FOLDER) {
        return false;
      }

      IFolder metaInfFolder = (IFolder) metaInf;
      if (metaInfFolder.members().length != 1) {
        return false;
      }
      IResource manifestMf = metaInfFolder.findMember("/MANIFEST.MF");
      if (manifestMf == null || manifestMf.getType() != IResource.FILE) {
        return false;
      }

      IFolder webInfFolder = (IFolder) webInf;
      if (webInfFolder.members().length != 2) {
        return false;
      }
      IResource lib = webInfFolder.findMember("/lib");
      if (lib == null || lib.getType() != IResource.FOLDER) {
        return false;
      }
      IResource webXml = webInfFolder.findMember("/web.xml");
      if (webXml == null || webXml.getType() != IResource.FILE) {
        return false;
      }

      if (((IFolder) lib).members().length != 0) {
        return false;
      }
      return isWtpGeneratedWebXml(webXml);

    } catch (CoreException ex) {
      return false;
    }
  }

  private static boolean isWtpGeneratedWebXml(IResource webXml) {
    try {
      // Check web.xml has only one top-level <web-app> and nothing else.
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      try (InputStream inputStream = new FileInputStream(webXml.getLocation().toFile())) {
        Document document = builder.parse(inputStream);
        Element root = document.getDocumentElement();
        if (!"web-app".equals(root.getTagName())) {
          return false;
        }

        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
          System.out.println(node.getNodeName() + ", type: " + node.getNodeType() + ", value: " + node.getNodeValue());
        }
      }
      return true;

    } catch (ParserConfigurationException | IOException | SAXException ex) {
      return false;
    }
  }

}
