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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.Rule;
import org.junit.Test;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;

public class ToServlet25SourceQuickFixTest {
    
  private static final IProjectFacetVersion APPENGINE_STANDARD_FACET_VERSION_1 =
      ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID).getVersion("1");
  private static final String SERVLET_MARKER =
      "com.google.cloud.tools.eclipse.appengine.validation.servletMarker";
  @Rule
  public TestProjectCreator appEngineStandardProject =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
          APPENGINE_STANDARD_FACET_VERSION_1);
  
  @Test
  public void testConvertServlet() throws CoreException {
    IProject project = appEngineStandardProject.getProject();
    IFile file = project.getFile("web.xml");
    String webXml = "<web-app xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" version='3.1'/>";
    file.create(ValidationTestUtils.stringToInputStream(webXml), IFile.FORCE, null);

    IWorkbench workbench = PlatformUI.getWorkbench();
    IEditorPart editorPart = WorkbenchUtil.openInEditor(workbench, file);
    ITextViewer viewer = ValidationTestUtils.getViewer(file);
    String preContents = viewer.getDocument().get();

    assertTrue(preContents.contains("version='3.1'"));

    XsltSourceQuickFix quickFix = new ToServlet25SourceQuickFix();
    quickFix.apply(viewer, 'a', 0, 0);

    IDocument document = viewer.getDocument();
    String contents = document.get();
    assertFalse(contents.contains("version='3.1'"));

    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1527
    editorPart.doSave(new NullProgressMonitor());

    ProjectUtils.waitForProjects(project);
    assertEquals(0, file.findMarkers(SERVLET_MARKER, true, IResource.DEPTH_ZERO).length);
  }

}
