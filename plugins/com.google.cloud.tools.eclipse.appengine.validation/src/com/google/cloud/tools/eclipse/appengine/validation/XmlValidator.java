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

import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.validation.AbstractValidator;
import org.eclipse.wst.validation.ValidationEvent;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Contains the logic for build validation and marker creation. The actual validation logic is
 * delegated to an {@link XmlValidationHelper}.
 */
public class XmlValidator extends AbstractValidator implements IExecutableExtension {

  private static final Logger logger = Logger.getLogger(XmlValidator.class.getName());

  private XmlValidationHelper helper;

  /**
   * Extracts byte[] from XML.
   */
  @Override
  public ValidationResult validate(ValidationEvent event, ValidationState state,
      IProgressMonitor monitor) {
    IFile file = (IFile) event.getResource();
    try (InputStream in = file.getContents()) {
        byte[] bytes = ByteStreams.toByteArray(in);
        validate(file, bytes);
    } catch (IOException | CoreException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
    return new ValidationResult();
  }

  /**
   * Clears all problem markers from the resource, then adds a marker to
   * the resource for every {@link ElementProblem} found in the file.
   */
  void validate(IFile resource, byte[] bytes) throws CoreException, IOException {
    try {
      deleteMarkers(resource);
      Document document = PositionalXmlScanner.parse(bytes);
      if (document != null) {
        List<ElementProblem> problems = helper.checkForProblems(resource, document);
        String encoding = (String) document.getDocumentElement().getUserData("encoding");
        Map<ElementProblem, Integer> problemOffsetMap =
            ValidationUtils.getOffsetMap(bytes, problems, encoding);
        for (Map.Entry<ElementProblem, Integer> entry : problemOffsetMap.entrySet()) {
          createMarker(resource, entry.getKey());
        }
      }
    } catch (SAXException ex) {
      // Do nothing; Eclipse notifies users of general SAX errors.
    }
  }

  /**
   * Creates an instance of the helper {@link XmlValidationHelper} and sets its
   * own helper to this instance.
   */
  @Override
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
      throws CoreException {
    try {
      if (!(data instanceof String)) {
        throw new CoreException(StatusUtil.error(getClass(), "Data must be a class name"));
      }
      String className = (String) data;
      Class<?> clazz = Class.forName(className);
      // We delegate the validation to a helper class that is specified in this extension's data
      // string. As such we can't use createExecutableExtension() and must instead resolve and
      // instantiate the helper directly.  As our validation helpers are all defined in this
      // bundle we can just use Class#forName(), though a general solution would require resolving
      // the class-name using the extension's defining bundle.
      setHelper((XmlValidationHelper) clazz.newInstance());
    } catch (ClassNotFoundException
        | SecurityException
        | InstantiationException
        | IllegalAccessException
        | IllegalArgumentException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
      throw new CoreException(StatusUtil.error(this, "Unable to instantiate helper", ex));
    }
  }

  @VisibleForTesting
  void setHelper(XmlValidationHelper helper) {
    this.helper = helper;
  }

  static void deleteMarkers(IResource resource) throws CoreException {
    resource.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
  }

  /**
   * Creates a marker from a given {@link ElementProblem}
   */
  static void createMarker(IResource resource, ElementProblem problem)
      throws CoreException {
    IMarker marker = resource.createMarker(problem.getMarkerId());
    marker.setAttribute(IMarker.SEVERITY, problem.getIMarkerSeverity());
    marker.setAttribute(IMarker.MESSAGE, problem.getMessage());
    int lineNumber = problem.getStart().getLineNumber();
    marker.setAttribute(IMarker.LOCATION, "line " + lineNumber);
    marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
  }

}
