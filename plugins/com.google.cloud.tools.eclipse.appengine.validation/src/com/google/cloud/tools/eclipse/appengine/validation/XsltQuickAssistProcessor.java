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

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * Provides quick assists for source editor.
 */
public class XsltQuickAssistProcessor implements IQuickAssistProcessor, IExecutableExtension {

  private String annotationText;
  private ICompletionProposal fix;
  
  @Override
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
      throws CoreException {
    String extensionData = (String) data;
    String[] splitData = extensionData.split(":");
    if (splitData.length == 3) {  // Number of identifiers in XsltQuickAssistProcessor
      //TODO
    }
  }
  
  XsltQuickAssistProcessor(String annotationType, XsltSourceQuickFix fix) {
    this.annotationText = annotationType;
    this.fix = fix;
  }
  
  @Override
  public ICompletionProposal[] computeQuickAssistProposals(
      IQuickAssistInvocationContext invocationContext) {
    ISourceViewer viewer = invocationContext.getSourceViewer();
    IAnnotationModel annotationModel = viewer.getAnnotationModel();
    Iterator iterator = annotationModel.getAnnotationIterator();
    while (iterator.hasNext()) {
      Object next = iterator.next();
      if (next instanceof Annotation) {
        Annotation annotation = (Annotation) next;
        if (annotation.getText().equals(annotationText)) {
          return new ICompletionProposal[] {fix};
        }
      } 
    }
    return null;
  }
  
  @Override
  public String getErrorMessage() {
    return null;
  }

  @Override
  public boolean canFix(Annotation annotation) {
    return !annotation.isMarkedDeleted();
  }

  @Override
  public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
    return false;
  }
  
}