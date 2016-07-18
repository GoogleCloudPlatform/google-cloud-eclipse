package com.google.cloud.tools.eclipse.appengine.deploy;

import java.io.File;
import java.io.IOException;

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
 * Parses and returns project ID and version from appengine-web.xml
 */
public class AppEngineDeployInfo {

  private static final String WEB_XML_NS_URI = "http://appengine.google.com/ns/1.0";
  private Document document;

  public void parse(File appEngineXml) throws CoreException {
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      document = documentBuilderFactory.newDocumentBuilder().parse(appEngineXml);
    } catch (IOException | SAXException | ParserConfigurationException exception) {
      throw new CoreException(new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass()).getSymbolicName(),
                                         Messages.getString("cannot.parse.appengine.xml"),
                                         exception)); //$NON-NLS-1$
    }
  }

  /**
   * @return project ID parsed from the &lt;application&gt; element of the appengine-web.xml or null if it is missing
   * @throws CoreException if parsing the value fails
   */
  public String getProjectId() throws CoreException {
    return getTopLevelValue(document, "appengine-web-app", "application"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * @return project version parsed from the &lt;version&gt; element of the appengine-web.xml or null if it is missing
   * @throws CoreException if parsing the value fails
   */
  public String getProjectVersion() throws CoreException {
    return getTopLevelValue(document, "appengine-web-app", "version"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private String getTopLevelValue(Document doc, String parentTagName, String childTagName) throws CoreException {
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
      throw new CoreException(new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass()).getSymbolicName(),
                                         Messages.getString("missing.appengine.xml.element") + childTagName,
                                         exception)); //$NON-NLS-1$
    }
  }
}
