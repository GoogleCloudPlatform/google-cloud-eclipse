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

import static org.junit.Assert.assertEquals;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.TextInvocationContext;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.junit.Rule;
import org.junit.Test;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;

public class AbstractQuickAssistProcessorTest {
  
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();
  
  @Test
  public void testComputeQuickAssistProposals() throws CoreException {
    
    IProject project = projectCreator.getProject();
    IFile file = project.getFile("testdata.xml");
    file.create(ValidationTestUtils.stringToInputStream("test"), IFile.FORCE, null);
    
    IWorkbench workbench = PlatformUI.getWorkbench();
    WorkbenchUtil.openInEditor(workbench, file);
    ISourceViewer viewer = (ISourceViewer) ValidationTestUtils.getViewer(file);
    
    IAnnotationModel model = viewer.getAnnotationModel();
    String annotationMessage = Messages.getString("application.element");
    model.addAnnotation(new Annotation("type", false, annotationMessage), new Position(1));
    
    TextInvocationContext context = new TextInvocationContext(viewer, 1, 1);
    AbstractQuickAssistProcessor processor = new ApplicationQuickAssistProcessor();
    ICompletionProposal[] proposals = processor.computeQuickAssistProposals(context);
    assertEquals(1, proposals.length);
  }
}