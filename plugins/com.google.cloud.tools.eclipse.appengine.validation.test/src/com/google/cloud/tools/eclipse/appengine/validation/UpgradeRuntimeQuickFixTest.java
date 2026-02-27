/*
 * Copyright 2018 Google LLC.
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

public class UpgradeRuntimeQuickFixTest {

  private static final String MARKER =
      "com.google.cloud.tools.eclipse.appengine.validation.runtimeMarker";

  @Rule
  public ThreadDumpingWatchdog timer = new ThreadDumpingWatchdog(2, TimeUnit.MINUTES);

  @Rule
  public TestProjectCreator appEngineStandardProject = new TestProjectCreator().withFacets(
      JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25, AppEngineStandardFacet.JRE7);

  @Test
  public void testConvertJava7() throws CoreException {
    String appengineWebAppJava7 =
        "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
        + "  <runtime>java7</runtime>"
        + "</appengine-web-app>";
    
    checkUpgrade(appengineWebAppJava7);
  }
  
  @Test
  public void testAddMissingRuntime() throws CoreException {
    String appengineWebAppJava7 =
        "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
        + "</appengine-web-app>";
    
    checkUpgrade(appengineWebAppJava7);
  }

  private void checkUpgrade(String appengineWebAppJava7) throws CoreException {
    IProject project = appEngineStandardProject.getProject();
    IFile file = project.getFile("appengine-web.xml");
    
    file.create(ValidationTestUtils.stringToInputStream(appengineWebAppJava7), IFile.FORCE, null);

    IWorkbench workbench = PlatformUI.getWorkbench();
    IEditorPart editorPart = WorkbenchUtil.openInEditor(workbench, file);
    ITextViewer viewer = ValidationTestUtils.getViewer(file);
    while (workbench.getDisplay().readAndDispatch()) {
      // spin the event loop
    }

    IMarker[] markers = ProjectUtils.waitUntilMarkersFound(file, MARKER,
        true /* includeSubtypes */, IResource.DEPTH_ZERO);
    assertEquals(1, markers.length);

    XsltSourceQuickFix quickFix = new UpgradeRuntimeSourceQuickFix();
    quickFix.apply(viewer, 'a', 0, 0);

    IDocument document = viewer.getDocument();
    String contents = document.get();
    assertThat(contents, not(containsString("java7")));
    assertThat(contents, containsString("  <runtime>java8</runtime>"));

    // https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/1527
    editorPart.doSave(new NullProgressMonitor());
  }

}
