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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
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

/**
 * Abstract source view validator.
 */
public abstract class AbstractXmlSourceValidator implements ISourceValidator, IValidator {
  
  private static final Logger logger = Logger.getLogger(
      AbstractXmlSourceValidator.class.getName());
  
  private IDocument document;
  
  /**
   * Validates a given {@link IDocument} if the project has the App Engine Standard facet.
   */
  public void validate(IValidationContext helper, IReporter reporter) throws ValidationException {
    IProject project = getProject(helper);
    try {
      IFacetedProject facetedProject = ProjectFacetsManager.create(project);
      if (AppEngineStandardFacet.hasAppEngineFacet(facetedProject)) {
        String encoding = getDocumentEncoding(document);
        byte[] bytes = document.get().getBytes(encoding);
        IFile source = getFile(helper);
        this.validate(reporter, source, bytes);
      }
    } catch (IOException | CoreException | ParserConfigurationException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }
  
  /**
   * Adds an {@link IMessage} to the XML file for every 
   * {@link BannedElement} found in the file.
   */
  protected abstract void validate(IReporter reporter, IFile source, byte[] bytes) 
      throws CoreException, IOException, ParserConfigurationException;
  
  /**
   * Creates a message from a given {@link BannedElement}.
   */
  void createMessage(IReporter reporter, BannedElement element, int elementOffset,
      String markerId, int severity) throws CoreException {
    IMessage message = new LocalizedMessage(severity, element.getMessage());
    message.setTargetObject(this);
    message.setMarkerId(markerId);
    message.setLineNo(element.getStart().getLineNumber());
    message.setOffset(elementOffset);
    message.setLength(element.getLength());
    reporter.addMessage(this, message);
  }
  
  /**
   * Returns the underlying IProject from a given IValidationContext or
   * null if the IValidationContext does not return any files that need
   * to be validated.
   */
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
  static IFile getFile(IValidationContext helper) {
    String[] fileUri = helper.getURIs();
    if (fileUri.length > 0) {
      IFile file = getFile(fileUri[0]);
      return file;
    }
    return null;
  }
  
  
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