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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Locator2;

import com.google.common.annotations.VisibleForTesting;

/**
 * Adds <groupId> element to {@link BannedElement} queue if the Maven plugin is not up to date.
 */
class PomXmlScanner extends AbstractScanner {

  private boolean foundBuild;
  private boolean foundGroupId;
  private StringBuffer groupIdContents;
  private int lineNumber;
  private int columnNumber;
  
  /**
   * Checks for opening <build> and <groupId> elements.
   */
  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    Locator2 locator = getLocator();
    if ("build".equalsIgnoreCase(localName)) {
      foundBuild = true;
    } else if (foundBuild && "groupId".equalsIgnoreCase(localName)) {
      foundGroupId = true;
      groupIdContents = new StringBuffer();
      lineNumber = locator.getLineNumber();
      columnNumber = locator.getColumnNumber();
    }
  }
  
  /**
   * Retrieves the contents of the <groupId> element.
   */
  @Override
  public void characters (char ch[], int start, int length)
      throws SAXException {
    if (foundGroupId) {
      groupIdContents.append(ch, start, length);
    }
  }
  
  /**
   * Checks for closing <build> and <groupId> elements. If a closing <groupId> element is found
   * with a parent <build> element and the Maven plugin version is out of date, a
   * {@link BannedElement} is added to the blacklist queue.
   */
  @Override
  public void endElement (String uri, String localName, String qName)
      throws SAXException {
    if ("build".equalsIgnoreCase(localName)) {
      foundBuild = false;
    } else if (foundBuild && "groupId".equals(localName)) {
      foundGroupId = false;
      if ("com.google.appengine".equals(groupIdContents.toString())) {
        DocumentLocation start = new DocumentLocation(lineNumber,
            columnNumber - qName.length() - 2);
        String message = Messages.getString("maven.plugin");
        BannedElement element = new BannedElement(message, start, qName.length() + 2);
        addToBlacklist(element);
      }
    }
  }
  
  @VisibleForTesting
  boolean getFoundBuild() {
    return foundBuild;
  }
  
  @VisibleForTesting
  boolean getFoundGroupId() {
    return foundGroupId;
  }
  
}
