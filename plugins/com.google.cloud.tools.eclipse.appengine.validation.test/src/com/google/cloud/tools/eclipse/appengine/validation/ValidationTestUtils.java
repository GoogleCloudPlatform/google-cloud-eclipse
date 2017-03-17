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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;

public class ValidationTestUtils {
  
  static IDocument getDocument(IFile file) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    WorkbenchUtil.openInEditor(workbench, file);
    ITextViewer viewer = ValidationTestUtils.getViewer(file);
    return viewer.getDocument();
  }
  
  static ITextViewer getViewer(IFile file) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
    IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
    IEditorPart editorPart = ResourceUtil.findEditor(activePage, file);
    
    ITextOperationTarget target =
        (ITextOperationTarget) editorPart.getAdapter(ITextOperationTarget.class);
    if (target instanceof ITextViewer) {
      return (ITextViewer) target;
    }
    return null;
  }
  
  static InputStream stringToInputStream(String string) {
    return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
  }
  
  static void createFolders(IContainer parent, IPath path) throws CoreException {
    if (!path.isEmpty()) {
      IFolder folder = parent.getFolder(new Path(path.segment(0)));
      if (!folder.exists()) {
        folder.create(true,  true,  null);
      }
      createFolders(folder, path.removeFirstSegments(1));
    }
  }
  
}