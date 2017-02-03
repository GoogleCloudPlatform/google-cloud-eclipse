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

/**
 * Class that represents a blacklisted element found in a project
 * XML file.
 */
public class BannedElement {

  private final String message;
  private final DocumentLocation start;
  private DocumentLocation end;
  private final int length;
  
  public BannedElement(String elementName, DocumentLocation start,
      DocumentLocation end, int length) {
    this.message = (elementName != null) ?
        AppEngineWebBlacklist.getBlacklistElementMessage(elementName) : "";
    this.start = (start != null) ? start : new DocumentLocation(0, 0);
    this.end = (end != null) ? end : new DocumentLocation(0, 0);
    this.length = length;
  }
  
  public BannedElement(String elementName, DocumentLocation start, int length) {
    this.message = (elementName != null) ?
        AppEngineWebBlacklist.getBlacklistElementMessage(elementName) : "";
    this.start = (start != null) ? start : new DocumentLocation(0, 0);
    this.length = length;
  }
  
  public BannedElement(String elementName) {
    this.message = (elementName != null) ? 
        AppEngineWebBlacklist.getBlacklistElementMessage(elementName) : "";
    this.start = new DocumentLocation(0, 0);
    this.end = new DocumentLocation(0, 0);
    this.length = 0;
  }
  
  public String getMessage() {
    return message;
  }
  
  public DocumentLocation getStart() {
    return start;
  }
  
  public DocumentLocation getEnd() {
    return end;
  }
  
  public int getLength() {
    return length;
  }
  
  public void setEnd(DocumentLocation end) {
    this.end = end;
  }
}