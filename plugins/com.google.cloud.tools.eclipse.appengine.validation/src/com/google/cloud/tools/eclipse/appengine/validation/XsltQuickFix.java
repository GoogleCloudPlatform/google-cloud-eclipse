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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

/**
 * Applies XSLT quick fix.
 */
public class XsltQuickFix implements IMarkerResolution {

  private static final Logger logger = Logger.getLogger(XsltQuickFix.class.getName());
  private String xsltPath;
  private String message;

  public XsltQuickFix(String xsltPath, String message) {
    this.xsltPath = xsltPath;
    this.message = message;
  }

  @Override
  public String getLabel() {
    return message;
  }

  /**
   * Attempts to edit the {@link IDocument} in the open editor. If the editor is not open,
   * reads the file from memory and transforms in place.
   */
  @Override
  public void run(IMarker marker) {
    try {
      IFile file = (IFile) marker.getResource();
      IDocument document = getCurrentDocument(file);
      URL xslPath = ApplicationQuickFix.class.getResource(xsltPath);
      if (document != null) {
        String currentContents = document.get();
        try (Reader documentReader = new StringReader(currentContents);
            InputStream stylesheet = xslPath.openStream();
            InputStream transformed = Xslt.applyXslt(documentReader, stylesheet)) {
          String encoding = file.getCharset();
          String newDoc = ValidationUtils.convertStreamToString(transformed, encoding);
          document.set(newDoc);
        }
      } else {
        Xslt.transformInPlace(file, xslPath);
      }
      marker.delete();
    } catch (IOException | TransformerException | CoreException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }

  /**
   * Returns {@link IDocument} in the open editor, or null if the editor
   * is not open.
   */
  static IDocument getCurrentDocument(IFile file) {
    try {
      IWorkbench workbench = PlatformUI.getWorkbench();
      IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
      IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
      IEditorPart editorPart = ResourceUtil.findEditor(activePage, file);
      if (editorPart != null) {
        IDocument document = editorPart.getAdapter(IDocument.class);
        return document;
      }
      return null;
    } catch (IllegalStateException ex) {
      //If workbench does not exist
      return null;
    }
  }

}