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

import java.util.Objects;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import com.google.common.base.Preconditions;

/**
 * An element that will receive a problem marker. 
 */
class ElementProblem {

  private final String message;
  private final DocumentLocation start;
  private final int length;
  private final String markerId;
  private final int iMarkerSeverity;
  private final int iMessageSeverity;
  private IQuickAssistProcessor processor;

  /**
   * @param length the length of the marker underline. Length == 0 results in a
   *        marker in the vertical ruler and no underline.
   */
  ElementProblem(String message, String markerId, int iMarkerSeverity, int iMessageSeverity,
      DocumentLocation start, int length, IQuickAssistProcessor processor) {
    Preconditions.checkNotNull(message, "element name is null");
    Preconditions.checkNotNull(markerId, "markerId is null");
    Preconditions.checkNotNull(start, "start is null");
    Preconditions.checkArgument(length >= 0, "length < 0");
    this.message = message;
    this.start = start;
    this.length = length;
    this.markerId = markerId;
    this.iMarkerSeverity = iMarkerSeverity;
    this.iMessageSeverity = iMessageSeverity;
    this.processor = processor;
  }

  /**
   * A problem with no specific location in the document such as an element
   * that should be present but isn't.
   */
  ElementProblem(String message, String markerId) {
    this(message, markerId,
        IMarker.SEVERITY_WARNING, IMessage.NORMAL_SEVERITY, new DocumentLocation(0, 0), 1, null);
  }

  String getMessage() {
    return message;
  }

  DocumentLocation getStart() {
    return start;
  }

  int getLength() {
    return length;
  }
  
  String getMarkerId() {
    return markerId;
  }
  
  int getIMarkerSeverity() {
    return iMarkerSeverity;
  }

  int getIMessageSeverity() {
    return iMessageSeverity;
  }
  
  IQuickAssistProcessor getQuickAssistProcessor() {
    return processor;
  }
  
  /**
   * ElementProblems are equal if they represent the same marker type (marker ID),
   * have the same location within a document, and display the same message.
   */
  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (object == null || !(object instanceof ElementProblem)) {
      return false;
    } 
    ElementProblem element = (ElementProblem) object;
    return Objects.equals(markerId, element.getMarkerId()) &&
        Objects.equals(message, element.getMessage()) &&
        start.getLineNumber() == element.getStart().getLineNumber() &&
        start.getColumnNumber() == element.getStart().getColumnNumber();
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(markerId, message, start.getLineNumber(), start.getColumnNumber());
  }
  
}