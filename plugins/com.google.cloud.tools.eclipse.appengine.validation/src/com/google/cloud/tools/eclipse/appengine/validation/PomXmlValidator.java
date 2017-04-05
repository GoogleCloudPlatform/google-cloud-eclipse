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

package com.google.cloud.tools.eclipse.appengine.validation;

import java.util.ArrayList;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.resources.IResource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PomXmlValidator implements XmlValidationHelper {

  private static final XPathFactory FACTORY = XPathFactory.newInstance();

  /**
   * Selects all the <groupId> elements with value "com.google.appengine" whose <artifactId>
   * sibling has the value "appengine-maven-plugin" or "gcloud-maven-plugin".
   */
  @Override
  public ArrayList<BannedElement> checkForElements(IResource resource, Document document) {
    ArrayList<BannedElement> blacklist = new ArrayList<>();
    try {
      XPath xPath = FACTORY.newXPath();
      NamespaceContext nsContext = new MavenContext();
      xPath.setNamespaceContext(nsContext);
      String selectGroupId = "//prefix:plugin/prefix:groupId[.='com.google.appengine']"
          + "[../prefix:artifactId[text()='appengine-maven-plugin'"
          + " or text()='gcloud-maven-plugin']]";
      NodeList groupIdElements =
          (NodeList) xPath.compile(selectGroupId).evaluate(document, XPathConstants.NODESET);
      for (int i = 0; i < groupIdElements.getLength(); i++) {
        Node child = groupIdElements.item(i);
        DocumentLocation location = (DocumentLocation) child.getUserData("location");
        BannedElement element = new MavenPluginElement(location, child.getTextContent().length());
        blacklist.add(element);
      }
    } catch (XPathExpressionException ex) {
      throw new RuntimeException("Invalid XPath expression");
    }
    return blacklist;
  }

  @Override
  public String getXsd() {
    return null;
  }
  
}