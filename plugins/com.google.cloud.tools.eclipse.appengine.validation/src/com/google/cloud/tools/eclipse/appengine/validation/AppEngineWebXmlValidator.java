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

/**
 * Validator for appengine-web.xml
 */
public class AppEngineWebXmlValidator implements XmlValidationHelper {

  @Override
  public ArrayList<BannedElement> checkForElements(IResource resource, Document document) {
    ArrayList<BannedElement> blacklist = new ArrayList<>();
    ArrayList<String> blacklistedElements = AppEngineWebBlacklist.getBlacklistElements();
    for (String elementName : blacklistedElements) {
      NodeList nodeList =
          document.getElementsByTagNameNS("http://appengine.google.com/ns/1.0", elementName);
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        DocumentLocation userData = (DocumentLocation) node.getUserData("location");
        AppEngineBlacklistElement element = new AppEngineBlacklistElement(
            elementName,
            userData,
            node.getTextContent().length());
        blacklist.add(element);
      }
    }
    return blacklist;
  }
}
