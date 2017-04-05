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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A blacklisted element that will receive a problem marker. 
 */
class SaxErrorHandler implements ErrorHandler {

  private static final Logger logger =
      Logger.getLogger(SaxErrorHandler.class.getName());
  IResource resource;
  
  SaxErrorHandler(IResource resource) {
    this.resource = resource;
  }
  
  @Override
  public void warning(SAXParseException exception) throws SAXException {
    // Do nothing
  }

  @Override
  public void error(SAXParseException exception) {
    try {
      createSaxErrorMessage(resource, exception);
    } catch (CoreException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }

  @Override
  public void fatalError(SAXParseException exception) {
    try {
      createSaxErrorMessage(resource, exception);
    } catch (CoreException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }
  
  /**
   * Sets error marker where SAX parser fails.
   */
  static void createSaxErrorMessage(IResource resource, SAXParseException ex) throws CoreException {
    int lineNumber = ex.getLineNumber();
    IMarker marker = resource.createMarker("org.eclipse.core.resources.problemmarker");
    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
    marker.setAttribute(IMarker.MESSAGE, ex.getMessage());
    marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
    marker.setAttribute(IMarker.LOCATION, "line " + lineNumber);
  }
  
}