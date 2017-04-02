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

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;

/**
 * Provides quick assists for source editor.
 */
public abstract class AbstractQuickAssistProcessor implements IQuickAssistProcessor {

  private ICompletionProposal fix;
  
  AbstractQuickAssistProcessor(XsltSourceQuickFix fix) {
    this.fix = fix;
  }

  @Override
  public ICompletionProposal[] computeQuickAssistProposals(
      IQuickAssistInvocationContext invocationContext) {
    if (fix != null) {
      return new ICompletionProposal[] {fix};
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