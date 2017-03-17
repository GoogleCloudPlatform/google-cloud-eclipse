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

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
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

public class XsltSourceQuickFixTest {

  private static final String BLACKLIST_MARKER =
      "com.google.cloud.tools.eclipse.appengine.validation.appEngineBlacklistMarker";

  private static final String APPLICATION_XML =
      "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
      + "<application>"
      + "</application>"
      + "</appengine-web-app>";
  private static final IProjectFacetVersion APPENGINE_STANDARD_FACET_VERSION_1 =
      ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID).getVersion("1");

  @Rule
  public TestProjectCreator appEngineStandardProject =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
          APPENGINE_STANDARD_FACET_VERSION_1);

  @Test
  public void testApply() throws CoreException {

    IProject project = appEngineStandardProject.getProject();
    IFile file = project.getFile("appengine-web.xml");
    file.create(ValidationTestUtils.stringToInputStream(APPLICATION_XML), IFile.FORCE, null);

    IWorkbench workbench = PlatformUI.getWorkbench();
    IEditorPart editorPart = WorkbenchUtil.openInEditor(workbench, file);
    ITextViewer viewer = ValidationTestUtils.getViewer(file);
    String preContents = viewer.getDocument().get();

    assertTrue(preContents.contains("application"));

    ProjectUtils.waitForProjects(project);
    assertEquals(1, file.findMarkers(BLACKLIST_MARKER, true, IResource.DEPTH_ZERO).length);

    XsltSourceQuickFix quickFix = new XsltSourceQuickFix("/xslt/application.xsl",
        Messages.getString("remove.application.element"));
    quickFix.apply(viewer, 'a', 0, 0);

    IDocument document = viewer.getDocument();
    String contents = document.get();
    assertFalse(contents.contains("application"));

    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1527
    editorPart.doSave(new NullProgressMonitor());

    ProjectUtils.waitForProjects(project);
    assertEquals(0, file.findMarkers(BLACKLIST_MARKER, true, IResource.DEPTH_ZERO).length);
  }

}