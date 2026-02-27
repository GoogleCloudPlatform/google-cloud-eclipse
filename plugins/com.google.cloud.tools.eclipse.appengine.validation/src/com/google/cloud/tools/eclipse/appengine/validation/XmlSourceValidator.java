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

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.sse.core.internal.encoding.EncodingMemento;
import org.eclipse.wst.sse.core.internal.text.BasicStructuredDocument;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.ISourceValidator;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.operations.LocalizedMessage;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Contains the logic for source validation and message creation. The actual validation logic is
 * delegated to an {@link XmlValidationHelper}.
 */
public class XmlSourceValidator implements ISourceValidator, IValidator, IExecutableExtension {

  private static final Logger logger = Logger.getLogger(
      XmlSourceValidator.class.getName());

  private IDocument document;
  private XmlValidationHelper helper;

  /**
   * Validates a given {@link IDocument} if the project has the App Engine Standard facet.
   */
  @Override
  public void validate(IValidationContext helper, IReporter reporter) throws ValidationException {
    IProject project = getProject(helper);
    try {
      IFacetedProject facetedProject = ProjectFacetsManager.create(project);
      if (facetedProject != null && AppEngineStandardFacet.hasFacet(facetedProject)) {
        String encoding = getDocumentEncoding(document);
        byte[] bytes = document.get().getBytes(encoding);
        IFile source = getFile(helper);
        validate(reporter, source, bytes);
      }
    } catch (IOException | CoreException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }

  /**
   * Adds an {@link IMessage} to the XML file for every
   * {@link ElementProblem} found in the file.
   */
  @VisibleForTesting
  void validate(IReporter reporter, IFile source, byte[] bytes) throws IOException {
    try {
      Document document = PositionalXmlScanner.parse(bytes);
      if (document != null) {
        List<ElementProblem> problems = helper.checkForProblems(source, document);
        String encoding = (String) document.getDocumentElement().getUserData("encoding");
        Map<ElementProblem, Integer> problemOffsetMap =
            ValidationUtils.getOffsetMap(bytes, problems, encoding);
        for (Map.Entry<ElementProblem, Integer> entry : problemOffsetMap.entrySet()) {
          createMessage(reporter, entry.getKey(), entry.getValue());
        }
      }
    } catch (SAXException ex) {
      // Do nothing
      // Default Eclipse parser flags syntax errors
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
      // We delegate the validation to a helper class that is specified in this extension's data
      // string. As such we can't use createExecutableExtension() and must instead resolve and
      // instantiate the helper directly.  As our validation helpers are all defined in this
      // bundle we can just use Class#forName(), though a general solution would require resolving
      // the class-name using the extension's defining bundle.
      Class<?> clazz = Class.forName(className);
      this.setHelper((XmlValidationHelper) clazz.newInstance());
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

  /**
   * Creates a message from a given {@link ElementProblem}.
   */
  @VisibleForTesting
  void createMessage(IReporter reporter, ElementProblem problem, int offset) {
    IMessage message = new LocalizedMessage(problem.getIMessageSeverity(), problem.getMessage());
    message.setTargetObject(this);
    message.setMarkerId(problem.getMarkerId());
    // TODO offset by line
    int lineNumber = problem.getStart().getLineNumber() + 1;
    message.setLineNo(lineNumber);
    message.setOffset(offset);
    message.setLength(problem.getLength());
    message.setAttribute(IQuickAssistProcessor.class.getName(), problem.getQuickAssistProcessor());
    reporter.addMessage(this, message);
  }

  /**
   * Returns the underlying IProject from a given IValidationContext or
   * null if the IValidationContext does not return any files that need
   * to be validated.
   */
  @VisibleForTesting
  static IProject getProject(IValidationContext helper) {
    IFile file = getFile(helper);
    if (file != null) {
      return file.getProject();
    }
    return null;
  }

  /**
   * Returns the IFile for a given URI or null if the file does
   * not exist in the workspace.
   */
  @VisibleForTesting
  static IFile getFile(IValidationContext helper) {
    String[] fileUri = helper.getURIs();
    if (fileUri.length > 0) {
      IFile file = getFile(fileUri[0]);
      return file;
    }
    return null;
  }

  @VisibleForTesting
  static IFile getFile(String filePath) {
    IPath path = new Path(filePath);
    if (path.segmentCount() > 1) {
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      IFile file = root.getFile(path);
      if (file != null && file.exists()) {
        return file;
      }
    }
    return null;
  }

  static String getDocumentEncoding(IDocument document) {
    EncodingMemento encodingMemento = ((BasicStructuredDocument) document).getEncodingMemento();
    return encodingMemento.getDetectedCharsetName();
  }

  @Override
  public void cleanup(IReporter reporter) {
  }

  @Override
  public void connect(IDocument document) {
    this.document = document;
  }

  @Override
  public void disconnect(IDocument document) {
    this.document = null;
  }

  @Override
  public void validate(IRegion dirtyRegion, IValidationContext helper, IReporter reporter) {
  }

}