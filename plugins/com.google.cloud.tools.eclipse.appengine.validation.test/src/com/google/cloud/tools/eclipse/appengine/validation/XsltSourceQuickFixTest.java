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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.junit.Rule;
import org.junit.Test;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;

public class XsltSourceQuickFixTest {
  
  private static final String APPLICATION_XML =
      "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
      + "<application>"
      + "</application>"
      + "</appengine-web-app>";
  
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();
  
  @Test
  public void testApply() throws CoreException {
    
    IProject project = projectCreator.getProject();
    IFile file = project.getFile("testdata.xml");
    file.create(ValidationTestUtils.stringToInputStream(
      APPLICATION_XML), IFile.FORCE, null);
    
    IWorkbench workbench = PlatformUI.getWorkbench();
    WorkbenchUtil.openInEditor(workbench, file);
    ITextViewer viewer = ValidationTestUtils.getViewer(file);
    String preContents = viewer.getDocument().get();
    
    assertTrue(preContents.contains("application"));
    
    XsltSourceQuickFix quickFix = new XsltSourceQuickFix("/xslt/application.xsl",
        Messages.getString("remove.application.element"));
    quickFix.apply(viewer, 'a', 0, 0);
    
    IDocument document = viewer.getDocument();
    String contents = document.get();
    assertFalse(contents.contains("application"));
  }
  
}