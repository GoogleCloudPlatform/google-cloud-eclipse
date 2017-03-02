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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;

/**
 * Must be run as a plugin test.
 */
public class XsltQuickFixTest {
  
  private static final String APPLICATION_XML =
      "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
      + "<application>"
      + "</application>"
      + "</appengine-web-app>";
    
  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  private XsltQuickFix fix = new XsltQuickFix("/xslt/application.xsl",
      Messages.getString("remove.application.element"));

  @Test 
  public void testGetLabel() {
    assertEquals("Remove application element", fix.getLabel());
  }
  
  @Test
  public void testRun() throws IOException, ParserConfigurationException,
      SAXException, TransformerException, CoreException {

    IProject project = projectCreator.getProject();
    IFile file = project.getFile("testdata.xml");
    file.create(ValidationTestUtils.stringToInputStream(
      APPLICATION_XML), IFile.FORCE, null);
    
    IMarker marker = Mockito.mock(IMarker.class);
    Mockito.when(marker.getResource()).thenReturn(file);
        
    fix.run(marker);

    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    builderFactory.setNamespaceAware(true);
    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    Document transformed = builder.parse(file.getContents());
    assertEquals(0, transformed.getDocumentElement().getChildNodes().getLength());
  }
  
  @Test
  public void testGetCurrentDocument_existingEditor() throws CoreException {
    
    IProject project = projectCreator.getProject();
    IFile file = project.getFile("testdata.xml");
    file.create(ValidationTestUtils.stringToInputStream(
      APPLICATION_XML), IFile.FORCE, null);
    
    IWorkbench workbench = PlatformUI.getWorkbench();
    WorkbenchUtil.openInEditor(workbench, file);
    
    assertNotNull(XsltQuickFix.getCurrentDocument(file));
  }
  
  @Test
  public void testGetCurrentDocument_noEditor() throws CoreException {
    
    IProject project = projectCreator.getProject();
    IFile file = project.getFile("testdata.xml");
    file.create(ValidationTestUtils.stringToInputStream(
      APPLICATION_XML), IFile.FORCE, null);
    
    assertNull(XsltQuickFix.getCurrentDocument(file));
  }

}
