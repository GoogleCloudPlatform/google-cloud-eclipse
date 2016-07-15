package com.google.cloud.tools.eclipse.appengine.deploy;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class AppEngineDeployInfo {

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

  public String getProjectId() throws CoreException {
    return getTopLevelValue(document, "appengine-web-app", "application"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public String getProjectVersion() throws CoreException {
    return getTopLevelValue(document, "appengine-web-app", "version"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private String getTopLevelValue(Document doc, String parentTagName, String childTagName) throws CoreException {
    try {
      XPath xpath = XPathFactory.newInstance().newXPath();
      String expression = "/" + parentTagName + "/" + childTagName;
      Node widgetNode = (Node) xpath.evaluate(expression, doc, XPathConstants.NODE);
      if (widgetNode != null) {
        return widgetNode.getTextContent();
      } else {
        return null;
      }
    } catch (DOMException | XPathExpressionException exception) {
      throw new CoreException(new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass()).getSymbolicName(),
                                         Messages.getString("missing.appengine.xml.element") + childTagName,
                                         exception)); //$NON-NLS-1$
    }
  }
}
