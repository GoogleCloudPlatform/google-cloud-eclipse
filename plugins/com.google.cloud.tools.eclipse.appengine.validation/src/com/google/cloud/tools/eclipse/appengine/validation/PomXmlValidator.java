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

import org.eclipse.core.resources.IResource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PomXmlValidator implements XmlValidationHelper {

  @Override
  public ArrayList<BannedElement> checkForElements(IResource resource, Document document) {
    ArrayList<BannedElement> blacklist = new ArrayList<>();
    NodeList nodeList = document.getElementsByTagName("plugin");
    for (int i = 0; i < nodeList.getLength(); i++) {
      DocumentLocation location = null;
      Node node = nodeList.item(i);
      NodeList childNodes = node.getChildNodes();
      boolean foundGroupId = false;
      boolean foundArtifactId = false;
      String bannedElementText = "";
      for (int j = 0; j < childNodes.getLength(); j++) {
        Node pluginChild = childNodes.item(j);
        String localName = pluginChild.getNodeName();
        String nodeText = pluginChild.getTextContent();
        if ("groupId".equals(localName)) {
          if ("com.google.appengine".equals(nodeText)) {
            bannedElementText = nodeText;
            foundGroupId = true;
            location = (DocumentLocation) pluginChild.getUserData("location");
          }
        } else if ("artifactId".equals(localName)) {
          if ("appengine-maven-plugin".equals(nodeText)
              || "gcloud-maven-plugin".equals(nodeText)) {
            foundArtifactId = true;
          }
        }
      }
      if (foundGroupId && foundArtifactId) {
        BannedElement element = new MavenPluginElement(location, bannedElementText.length());
        blacklist.add(element);
      }
    }
    return blacklist;
  }
  
}