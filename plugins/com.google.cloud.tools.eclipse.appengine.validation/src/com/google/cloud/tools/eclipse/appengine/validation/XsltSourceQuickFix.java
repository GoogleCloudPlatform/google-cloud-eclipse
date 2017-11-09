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

import com.google.cloud.tools.eclipse.util.Xslt;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * XSLT quick fix available in source editor.
 */
public class XsltSourceQuickFix implements ICompletionProposal, ICompletionProposalExtension2 {

  private static final Logger logger = Logger.getLogger(XsltSourceQuickFix.class.getName());
  private String xsltPath;
  private String message;

  public XsltSourceQuickFix(String xsltPath, String message) {
    this.xsltPath = xsltPath;
    this.message = message;
  }

  /**
   * Applies the provided XSLT style sheet to the {@link IDocument} in the source editor.
   */
  @Override
  public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
    URL xslt = ApplicationQuickFix.class.getResource(xsltPath);
    IDocument document = viewer.getDocument();
    try (Reader in = new StringReader(document.get());
        InputStream stylesheetStream = xslt.openStream();
        InputStream transformed = Xslt.applyXslt(in, stylesheetStream)) {
      String encoding = XmlSourceValidator.getDocumentEncoding(document);
      String result = ValidationUtils.convertStreamToString(transformed, encoding);
      document.set(result);
    } catch (IOException | TransformerException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }

  @Override
  public String getDisplayString() {
    return message;
  }

  @Override
  public boolean validate(IDocument document, int offset, DocumentEvent event) {
    return true;
  }

  @Override
  public void apply(IDocument document) {
  }

  @Override
  public Point getSelection(IDocument document) {
    return null;
  }

  @Override
  public String getAdditionalProposalInfo() {
    return null;
  }

  @Override
  public Image getImage() {
    return null;
  }

  @Override
  public IContextInformation getContextInformation() {
    return null;
  }

  @Override
  public void selected(ITextViewer viewer, boolean smartToggle) {
  }

  @Override
  public void unselected(ITextViewer viewer) {
  }

}