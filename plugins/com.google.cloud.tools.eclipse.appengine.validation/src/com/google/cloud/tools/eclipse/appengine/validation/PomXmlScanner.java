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
 * Adds <groupId> element to {@link BannedElement} queue if a deprecated
 * App Engine Maven plugin is used.
 */
class PomXmlScanner extends AbstractScanner {

  private boolean insidePlugin;
  private boolean foundAppEngineGroupId;
  private boolean foundArtifactId;
  private boolean saveContents;
  private StringBuilder elementContents;
  private int lineNumber;
  private int columnNumber;
  
  /**
   * Checks for opening <build> and <groupId> tags within a <plugin> element.
   */
  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    if ("plugin".equalsIgnoreCase(localName)) {
      insidePlugin = true;
    } else if (insidePlugin && "artifactId".equalsIgnoreCase(localName)) {
      saveContents = true;
      elementContents = new StringBuilder();
    } else if (insidePlugin && "groupId".equalsIgnoreCase(localName)) {
      Locator2 locator = getLocator();
      saveContents = true;
      elementContents = new StringBuilder();
      lineNumber = locator.getLineNumber();
      columnNumber = locator.getColumnNumber();
    }
  }
  
  /**
   * Retrieves the contents of the <groupId> or <artifactId> element.
   */
  @Override
  public void characters (char ch[], int start, int length)
      throws SAXException {
    if (saveContents) {
      elementContents.append(ch, start, length);
    }
  }
  
  /**
   * Checks for closing <build> and <groupId> tags. If a closing <groupId> tag
   * with a deprecated App Engine Maven plugin and an <artifactId> tag with
   * value "appengine-maven-plugin" are found within the same <plugin>, a
   * {@link BannedElement} is added to the blacklist queue.
   */
  @Override
  public void endElement (String uri, String localName, String qName)
      throws SAXException {
    if ("plugin".equalsIgnoreCase(localName)) {
      // Found closing <plugin> tag
      resetFlags();
    } else if (insidePlugin && "groupId".equals(localName)) {
      // Found closing <groupId> tag with parent <plugin>
      saveContents = false;
      if ("com.google.appengine".equals(elementContents.toString())) {
        foundAppEngineGroupId = true;
      }
    } else if (insidePlugin && "artifactId".equalsIgnoreCase(localName)) {
      // Found closing <artifactId> tag with parent <plugin>
      saveContents = false;
      if ("appengine-maven-plugin".equals(elementContents.toString()) || 
          "gcloud-maven-plugin".equals(elementContents.toString())) {
        foundArtifactId = true;
      }
    }
    if (foundAppEngineGroupId && foundArtifactId) {
      // Found deprecated App Engine Maven plugin and App Engine artifact ID
      // with the same <plugin> parent
      DocumentLocation start = new DocumentLocation(lineNumber, columnNumber - 9);
      String message = Messages.getString("maven.plugin");
      BannedElement element = new MavenPluginElement(message, start, 0);
      addToBlacklist(element);
      foundAppEngineGroupId = false;
      foundArtifactId = false;
    }
  }
  
  /**
   * Resets all the position flags. 
   */
  private void resetFlags() {
    insidePlugin = false;
    foundAppEngineGroupId = false;
    foundArtifactId = false;
    saveContents = false;
  }
  
  @VisibleForTesting
  boolean getInsidePlugin() {
    return insidePlugin;
  }
  
  @VisibleForTesting
  boolean getSaveContents() {
    return saveContents;
  }
  
}
