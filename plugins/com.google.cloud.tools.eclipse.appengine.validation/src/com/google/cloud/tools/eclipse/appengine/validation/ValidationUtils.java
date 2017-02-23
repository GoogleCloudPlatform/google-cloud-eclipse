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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;
import com.google.common.io.CharStreams;
import java.util.Map;
import java.util.Queue;

/**
 * Utility methods for validating XML files.
 */
public class ValidationUtils {

  private static final Logger logger = Logger.getLogger(ValidationUtils.class.getName());
  
  /**
   * Creates a {@link Map} of {@link BannedElement}s and their respective document-relative
   * character offsets.
   */
  public static Map<BannedElement, Integer> getOffsetMap(byte[] bytes,
      SaxParserResults parserResults) {
    Queue<BannedElement> blacklist = parserResults.getBlacklist();
    Map<BannedElement, Integer> bannedElementOffsetMap = new HashMap<>();
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(bais, parserResults.getEncoding()))) {
      int currentLine = 1;
      int charOffset = 0;
      while (!blacklist.isEmpty()) {
        BannedElement element = blacklist.poll();
        while (element.getStart().getLineNumber() > currentLine) {
          String line = reader.readLine();
          charOffset += line.length() + 1;
          currentLine++;
        }
        int start = charOffset + element.getStart().getColumnNumber() - 1;
        bannedElementOffsetMap.put(element, start);
      }
    } catch (IOException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
    return bannedElementOffsetMap;
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
        IDocument document = (IDocument) editorPart.getAdapter(IDocument.class);
        return document;
      }
      return null;
    } catch (IllegalStateException ex) {
      //If workbench does not exist
      return null;
    }
  }
  
  static String convertStreamToString(InputStream is, String charset) throws IOException {
    String result = CharStreams.toString(new InputStreamReader(is, charset));
    return result;
  }
}