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
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Utilities to obtain information from appengine-web.xml.
 */
// TODO this class belongs in appengine-plugins-core
public class AppEngineDescriptor {

  private static final String WEB_XML_NS_URI = "http://appengine.google.com/ns/1.0";
  private Document document;
  
  private AppEngineDescriptor() {
    // force use of parse method
  }

  public static AppEngineDescriptor parse(InputStream appEngineXmlContents) throws CoreException {
    try {
      AppEngineDescriptor instance = new AppEngineDescriptor();
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      instance.document = documentBuilderFactory.newDocumentBuilder().parse(appEngineXmlContents);
      return instance;
    } catch (IOException | SAXException | ParserConfigurationException exception) {
      throw new CoreException(
          StatusUtil.error(AppEngineDescriptor.class, "Cannot parse appengine-web.xml", exception));
    }
  }

  /**
   * @return project ID parsed from the &lt;application&gt; element of the appengine-web.xml or null
   *         if it is missing
   * @throws CoreException if parsing the value fails
   */
  public String getProjectId() throws CoreException {
    return getTopLevelValue(document, "appengine-web-app", "application"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * @return project version parsed from the &lt;version&gt; element of the appengine-web.xml or
   *         null if it is missing
   * @throws CoreException if parsing the value fails
   */
  public String getProjectVersion() throws CoreException {
    return getTopLevelValue(document, "appengine-web-app", "version"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * @return service ID parsed from the &lt;service&gt; element of the appengine-web.xml, or
   *         null if it is missing. Will also look at module ID.
   * @throws CoreException if parsing the value fails
   */
  public String getServiceId() throws CoreException {
    String serviceID = getTopLevelValue(document, "appengine-web-app", "service"); //$NON-NLS-1$ //$NON-NLS-2$
    if (serviceID != null) {
      return serviceID;
    }
    return getTopLevelValue(document, "appengine-web-app", "module"); //$NON-NLS-1$ //$NON-NLS-2$
  }


  private String getTopLevelValue(Document doc, String parentTagName, String childTagName)
      throws CoreException {
    try {
      NodeList parentElements = doc.getElementsByTagNameNS(WEB_XML_NS_URI, parentTagName);
      if (parentElements.getLength() > 0) {
        Node parent = parentElements.item(0);
        if (parent.hasChildNodes()) {
          for (int i = 0; i < parent.getChildNodes().getLength(); ++i) {
            Node child = parent.getChildNodes().item(i);
            if (child.getNodeName().equals(childTagName)) {
              return child.getTextContent();
            }
          }
        }
      }
      return null;
    } catch (DOMException exception) {
      throw new CoreException(
          new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass()).getSymbolicName(),
              "Missing appengine-web element: " + childTagName, exception));
    }
  }
}
