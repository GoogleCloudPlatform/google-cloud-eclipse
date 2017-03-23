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
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.IncrementalHelper;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.IncrementalReporter;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.junit.Rule;
import org.junit.Test;

public class AbstractXmlSourceValidatorTest {

  private static final String APPLICATION_XML =
      "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
      + "<application>"
      + "</application>"
      + "</appengine-web-app>";
  private static final IProjectFacetVersion APPENGINE_STANDARD_FACET_VERSION_1 =
      ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID).getVersion("1");

  @Rule public TestProjectCreator dynamicWebProject =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25);
  @Rule
  public TestProjectCreator appEngineStandardProject =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
          APPENGINE_STANDARD_FACET_VERSION_1);

  @Test
  public void testValidate_appEngineStandardFacet() throws CoreException, ValidationException {
    IProject project = appEngineStandardProject.getProject();
    IFile file = project.getFile("testdata.xml");
    file.create(ValidationTestUtils.stringToInputStream(
        APPLICATION_XML), 0, null);

    IDocument document = ValidationTestUtils.getDocument(file);

    // Adds the URI of the file to be validated to the IncrementalHelper.
    IncrementalHelper helper = new IncrementalHelper(document, project);
    IPath path = file.getFullPath();
    helper.setURI(path.toString());

    AbstractXmlSourceValidator validator = new AppEngineWebXmlSourceValidator();
    validator.connect(document);
    IncrementalReporter reporter = new IncrementalReporter(null);
    validator.validate(helper, reporter);
    assertEquals(1, reporter.getMessages().size());
  }

  @Test
  public void testValidate_dynamicWebProject() throws CoreException, ValidationException {
    IProject project = dynamicWebProject.getProject();
    IFile file = project.getFile("testdata.xml");
    file.create(ValidationTestUtils.stringToInputStream(
        APPLICATION_XML), 0, null);

    IDocument document = ValidationTestUtils.getDocument(file);

    // Adds the URI of the file to be validated to the IncrementalHelper.
    IncrementalHelper helper = new IncrementalHelper(document, project);
    IPath path = file.getFullPath();
    helper.setURI(path.toString());

    AbstractXmlSourceValidator validator = new AppEngineWebXmlSourceValidator();
    validator.connect(document);
    IncrementalReporter reporter = new IncrementalReporter(null);
    validator.validate(helper, reporter);
    assertEquals(0, reporter.getMessages().size());
  }

  @Test
  public void getDocumentEncodingTest() throws CoreException {

    IProject project = appEngineStandardProject.getProject();
    IFile file = project.getFile("testdata.xml");
    file.create(ValidationTestUtils.stringToInputStream(
      APPLICATION_XML), IFile.FORCE, null);
    IDocument document = ValidationTestUtils.getDocument(file);

    assertEquals("UTF-8", AbstractXmlSourceValidator.getDocumentEncoding(document));
  }

  @Test
  public void testCreateMessage() throws CoreException {
    IncrementalReporter reporter = new IncrementalReporter(null /*progress monitor*/);
    AbstractXmlSourceValidator validator = new AppEngineWebXmlSourceValidator();
    BannedElement element = new BannedElement("message");
    validator.createMessage(reporter, element, 0, "", IMessage.NORMAL_SEVERITY);
    assertEquals(1, reporter.getMessages().size());
  }

  @Test
  public void testGetFile() throws CoreException {
    IProject project = appEngineStandardProject.getProject();
    IFile file = project.getFile("testdata.xml");
    file.create(ValidationTestUtils.stringToInputStream(
        APPLICATION_XML), 0, null);

    assertTrue(file.exists());

    IPath path = file.getFullPath();
    IFile testFile = AbstractXmlSourceValidator.getFile(path.toString());

    assertNotNull(testFile);
    assertEquals(file, testFile);
  }

  @Test
  public void testGetProject() throws CoreException {
    IProject project = appEngineStandardProject.getProject();
    IFile file = project.getFile("testdata.xml");
    file.create(ValidationTestUtils.stringToInputStream(
        APPLICATION_XML), 0, null);

    IDocument document = ValidationTestUtils.getDocument(file);

    IncrementalHelper helper = new IncrementalHelper(document, project);
    IPath path = file.getFullPath();
    helper.setURI(path.toString());

    IProject testProject = AbstractXmlSourceValidator.getProject(helper);
    assertNotNull(testProject);
    assertEquals(project, testProject);
  }

}