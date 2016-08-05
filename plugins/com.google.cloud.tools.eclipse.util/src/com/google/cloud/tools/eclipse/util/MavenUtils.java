package com.google.cloud.tools.eclipse.util;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
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


  private Document parse(InputStream pomXml) throws CoreException {
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      return documentBuilderFactory.newDocumentBuilder().parse(pomXml);
    } catch (IOException | SAXException | ParserConfigurationException exception) {
      throw new CoreException(StatusUtil.error(getClass(), "Cannot parse pom.xml", exception));
    }
  }

  public String getProperty(InputStream pomXml, String propertyName) throws CoreException {
    return getTopLevelValue(parse(pomXml), "properties", propertyName); //$NON-NLS-1$ //$NON-NLS-2$
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

}
