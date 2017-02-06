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

import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.annotations.VisibleForTesting;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * SAX parser handler class. Parses a given XML file and adds blacklisted
 * elements to a Stack.
 * 
 * Two stacks are used to maintain BannedElement order as they appear
 * in the document.
 */
class BlacklistScanner extends DefaultHandler {
  
  private Locator locator;
  private Stack<BannedElement> preBlacklist;
  private Stack<BannedElement> blacklist;
  
  @Override
  public void setDocumentLocator(Locator locator) {
    this.locator = locator;
  }
  
  /**
   * Ensures parser always starts with an empty queue.
   */
  @Override
  public void startDocument() throws SAXException {
    this.preBlacklist = new Stack<>();
    this.blacklist = new Stack<>();
  }
  
  /**
   * Adds blacklisted start element to stack. Adding 2 to length accounts
   * for the start and end angle brackets of the tag.
   */
  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    if (AppEngineWebBlacklist.contains(qName)) {
      DocumentLocation start = new DocumentLocation(locator.getLineNumber(),
          locator.getColumnNumber() - qName.length() - 2);
      BannedElement element = new BannedElement(qName, start, qName.length() + 2);
      preBlacklist.add(element);
    }
  }
  
  /**
   * Pops the corresponding starting BannedElement off the preBlacklist 
   * stack then pushes it to the final blacklist stack.
   * 
   * Will later use this method add end location to banned element.
   */
  @Override
  public void endElement(String uri, String localName, String qName) 
      throws SAXException {
    if (AppEngineWebBlacklist.contains(qName)) {
      BannedElement element = preBlacklist.pop();
      blacklist.add(element);
    }
  }

  Stack<BannedElement> getBlacklist() {
    return blacklist;
  }
  
  /**
   * Only used for testing.
   */
  @VisibleForTesting
  Stack<BannedElement> getPreBlacklist() {
    return preBlacklist;
  }
  
  @Override
  public void error(SAXParseException ex) throws SAXException {
    //nests ex to conserve exception line number
    throw new SAXException(ex.getMessage(), ex);
  }
  
  @Override
  public void fatalError(SAXParseException ex) throws SAXException {
    throw new SAXException(ex.getMessage(), ex);
  }

  @Override
  public void warning(SAXParseException exception) throws SAXException { //do nothing
  }
  
}