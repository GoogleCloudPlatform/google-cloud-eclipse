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
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.osgi.framework.Bundle;

import com.google.cloud.tools.eclipse.util.status.StatusUtil;

/**
 * Provides quick assists for source editor.
 */
public class XsltQuickAssistProcessor implements IQuickAssistProcessor, IExecutableExtension {

  private String annotationText;
  private ICompletionProposal fix;
  private Class<?> clazz;
  
  XsltQuickAssistProcessor(String annotationType, XsltSourceQuickFix fix) {
    this.annotationText = annotationType;
    this.fix = fix;
  }
  
  /**
   * 0-argument constructor required by the Eclipse Extension Registry. Not intended for normal use.
   * 
   * @see XsltQuickAssistProcessor(String, XsltSourceQuickFix)
   * @noreference use {@link XsltQuickAssistProcessor(String, XsltSourceQuickFix)} instead
   */
  public XsltQuickAssistProcessor() {}
  
  @Override
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
      throws CoreException {
    if (data == null || !(data instanceof String)) {
      throw new CoreException(StatusUtil.error(getClass(), "Data must be a class name"));
    }
    String className = (String) data;
    String bundleSymbolicName = config.getNamespaceIdentifier();
    Bundle bundle = Platform.getBundle(bundleSymbolicName);
    if (bundle == null) {
      throw new CoreException(StatusUtil.error(this, "Missing bundle " + bundleSymbolicName));
    }
    try {
      clazz = bundle.loadClass(className);
    } catch (ClassNotFoundException ex) {
       throw new CoreException(StatusUtil.error(this,
           "Could not load class " + className
           + " from bundle " + bundle.getSymbolicName(),
           ex));
    }
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