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

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.validation.AbstractValidator;
import org.eclipse.wst.validation.ValidationEvent;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Validator for appengine-web.xml
 */
public class AppEngineWebXmlValidator extends AbstractValidator {
  
  private static final Logger logger = Logger.getLogger(
      AppEngineWebXmlValidator.class.getName());
  
  /**
   * Extracts byte[] from appengine-web.xml. 
   */
  @Override
  public ValidationResult validate(ValidationEvent event, ValidationState state,
      IProgressMonitor monitor) {
    IResource resource = event.getResource();
    IFile file = (IFile) resource;
    try (InputStream in = file.getContents()) {
      byte[] bytes = ByteStreams.toByteArray(in);
      validate(resource, bytes);
      return new ValidationResult();
    } catch (IOException | CoreException | ParserConfigurationException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
      return new ValidationResult();
    }
  }
    
  /**
   * Clears all problem markers from the resource, then adds a marker to 
   * appengine-web.xml for every {@link BannedElement} found in the file.
   */
  static void validate(IResource resource, byte[] bytes) 
      throws CoreException, IOException, ParserConfigurationException {
    try {
      deleteApplicationMarkers(resource);
      SaxParserResults parserResults = BlacklistSaxParser.readXml(bytes);
      Map<BannedElement, Integer> bannedElementOffsetMap =
          ValidationUtils.getOffsetMap(bytes, parserResults);
      for (Map.Entry<BannedElement, Integer> entry : bannedElementOffsetMap.entrySet()) {
        createMarker(resource, entry.getKey(), entry.getValue());
      }
    } catch (SAXException ex) {
      createSaxErrorMessage(resource, ex);
    }
  }
  
  static void deleteApplicationMarkers(IResource resource) throws CoreException {
    resource.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
  }
  
  /**
   * Creates a marker from a given {@link BannedElement}
   */
  static void createMarker(IResource resource,
      BannedElement element, int elementOffset) throws CoreException {
    String markerID = "com.google.cloud.tools.eclipse.appengine.validation.blacklistMarker";
    IMarker marker = resource.createMarker(markerID);
    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
    marker.setAttribute(IMarker.MESSAGE, element.getMessage());
    marker.setAttribute(IMarker.LOCATION, "line " + element.getStart().getLineNumber());
    marker.setAttribute(IMarker.CHAR_START, elementOffset);
    marker.setAttribute(IMarker.CHAR_END, elementOffset + element.getLength());
  }
  
  /**
   * Sets error marker where SAX parser fails.
   */
  static void createSaxErrorMessage(IResource resource, SAXException ex) throws CoreException {
    int lineNumber = ((SAXParseException) ex.getException()).getLineNumber();
    IMarker marker = resource.createMarker("org.eclipse.core.resources.problemmarker");
    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
    marker.setAttribute(IMarker.MESSAGE, ex.getMessage());
    marker.setAttribute(IMarker.LINE_NUMBER,
        ((SAXParseException) ex.getException()).getLineNumber());
    marker.setAttribute(IMarker.LOCATION, "line " + lineNumber);
  }
  
}
