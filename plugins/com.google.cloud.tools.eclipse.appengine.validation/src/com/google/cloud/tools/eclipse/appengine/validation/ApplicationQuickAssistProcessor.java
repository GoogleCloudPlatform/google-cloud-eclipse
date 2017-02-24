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
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * Provides quick assists for source editor.
 */
public class ApplicationQuickAssistProcessor implements IQuickAssistProcessor {

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
        String annotationText = Messages.getString("application.element");
        if (annotation.getText().equals(annotationText)) {
          ICompletionProposal proposal = new ApplicationSourceQuickFix();
          return new ICompletionProposal[] {proposal};
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