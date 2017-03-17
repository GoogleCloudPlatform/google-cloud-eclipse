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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;

public class AbstractQuickAssistProcessorTest {
  
  private ISourceViewer viewer;
  private IAnnotationModel model;
  
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();
  
  
  @Before
  public void setUp() throws CoreException {
    IProject project = projectCreator.getProject();
    IFile file = project.getFile("testdata.xml");
    file.create(ValidationTestUtils.stringToInputStream("test"), IFile.FORCE, null);
    
    IWorkbench workbench = PlatformUI.getWorkbench();
    WorkbenchUtil.openInEditor(workbench, file);
    viewer = (ISourceViewer) ValidationTestUtils.getViewer(file);
    model = viewer.getAnnotationModel();
  }
  
  @Test
  public void testComputeApplicationQuickAssistProposals() {
    
    String applicationMessage = Messages.getString("application.element");
    model.addAnnotation(new Annotation("application", false, applicationMessage), new Position(1));
    TextInvocationContext applicationContext = new TextInvocationContext(viewer, 1, 1);
    AbstractQuickAssistProcessor processor = new ApplicationQuickAssistProcessor();
    ICompletionProposal[] proposals = processor.computeQuickAssistProposals(applicationContext);
    assertEquals(1, proposals.length);
    assertEquals("Remove application element", proposals[0].getDisplayString());
  }
  
  @Test
  public void testComputeVersionQuickAssistProposals() {
    String versionMessage = Messages.getString("version.element");
    model.addAnnotation(new Annotation("version", false, versionMessage), new Position(1));
    TextInvocationContext versionContext = new TextInvocationContext(viewer, 1, 1);
    AbstractQuickAssistProcessor versionProcessor = new VersionQuickAssistProcessor();
    ICompletionProposal[] versionProposals = versionProcessor.computeQuickAssistProposals(versionContext);
    assertEquals(1, versionProposals.length);
    assertEquals("Remove version element", versionProposals[0].getDisplayString());
  }
  
}