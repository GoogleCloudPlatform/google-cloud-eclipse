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

/**
 * Adds <web-app> element to {@link BannedElement} queue if the Servlet version is not 2.5.
 */
class WebXmlScanner extends AbstractScanner {

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    // Checks for expected namespace URI. Assume something else is going on if
    // web.xml has an unexpected root namespace.
    if ("web-app".equalsIgnoreCase(localName) && ("http://xmlns.jcp.org/xml/ns/javaee".equals(uri)
        || "http://java.sun.com/xml/ns/javaee".equals(uri))) {
      String version = attributes.getValue("version");
      if (!version.equals("2.5")) {
        Locator2 locator = getLocator();
        DocumentLocation start = new DocumentLocation(locator.getLineNumber(),
            locator.getColumnNumber());
        addToBlacklist(new JavaServletElement(Messages.getString("web.xml.version"), start, 0));
      }
    }
  }
  
}
