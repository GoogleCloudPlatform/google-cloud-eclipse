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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.ThreadDumpingWatchdog;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.ui.util.WorkbenchUtil;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
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
import org.junit.Rule;
import org.junit.Test;

public class ApplicationSourceQuickFixTest {

  private static final String MARKER =
      "com.google.cloud.tools.eclipse.appengine.validation.appEngineProblemMarker";

  private static final String APPLICATION_XML =
      "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
      + "<application>"
      + "</application>"
      + "</appengine-web-app>";

  @Rule
  public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  @Rule
  public TestProjectCreator appEngineStandardProject = new TestProjectCreator().withFacets(
      JavaFacet.VERSION_1_8, WebFacetUtils.WEB_25, AppEngineStandardFacet.JRE8);

  @Test
  public void testApply() throws CoreException {

    IProject project = appEngineStandardProject.getProject();
    IFile file = project.getFile("appengine-web.xml");
    file.create(ValidationTestUtils.stringToInputStream(APPLICATION_XML), IFile.FORCE, null);

    IWorkbench workbench = PlatformUI.getWorkbench();
    IEditorPart editorPart = WorkbenchUtil.openInEditor(workbench, file);
    ITextViewer viewer = ValidationTestUtils.getViewer(file);
    while (workbench.getDisplay().readAndDispatch()) {
      // spin the event loop
    }

    String preContents = viewer.getDocument().get();
    assertThat(preContents, containsString("application"));

    IMarker[] markers = ProjectUtils.waitUntilMarkersFound(file, MARKER,
        true /* includeSubtypes */, IResource.DEPTH_ZERO);
    assertEquals(1, markers.length);

    XsltSourceQuickFix quickFix = new ApplicationSourceQuickFix();
    quickFix.apply(viewer, 'a', 0, 0);

    IDocument document = viewer.getDocument();
    String contents = document.get();
    assertThat(contents, not(containsString("application")));
    assertThat(contents, not(containsString("?><appengine")));

    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1527
    editorPart.doSave(new NullProgressMonitor());

    ProjectUtils.waitUntilNoMarkersFound(file, MARKER, true /* includeSubtypes */,
        IResource.DEPTH_ZERO);
  }

}
