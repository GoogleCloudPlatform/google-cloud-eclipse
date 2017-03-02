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
 * Adds blacklisted start element to queue. Adding 2 to length accounts
 * for the start and end angle brackets of the tag.
 */
class BlacklistScanner extends AbstractScanner {

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    Locator2 locator = getLocator();
    if (AppEngineWebBlacklist.contains(qName)) {
      DocumentLocation start = new DocumentLocation(locator.getLineNumber(),
          locator.getColumnNumber() - qName.length() - 2);
      String message = AppEngineWebBlacklist.getBlacklistElementMessage(qName);
      BannedElement element = new AppEngineBlacklistElement(message, start, qName.length() + 2);
      addToBlacklist(element);
    }
  }
    
}
