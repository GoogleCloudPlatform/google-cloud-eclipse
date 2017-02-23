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

import com.google.common.base.Preconditions;

/**
 * A blacklisted element that will receive a problem marker. 
 */
public class BannedElement {

  private final String message;
  private final DocumentLocation start;
  private final int length;

  /**
   * @param length the length of the marker underline. Length == 0 results in a
   *        marker in the vertical ruler and no underline
   */
  public BannedElement(String message, DocumentLocation start, int length) {
    Preconditions.checkNotNull(message, "message is null");
    Preconditions.checkNotNull(start, "start is null");
    Preconditions.checkArgument(length >= 0, "length < 0");
    this.message = message;
    this.start = start;
    this.length = length;
  }

  public BannedElement(String message) {
    this(message, new DocumentLocation(0, 0), 0);
  }

  public String getMessage() {
    return message;
  }

  public DocumentLocation getStart() {
    return start;
  }

  public int getLength() {
    return length;
  }

}