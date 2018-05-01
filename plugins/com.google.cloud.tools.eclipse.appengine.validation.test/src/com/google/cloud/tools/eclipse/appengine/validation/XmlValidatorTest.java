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
import com.google.cloud.tools.eclipse.test.util.ArrayAssertions;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.validation.ValidationFramework;
import org.eclipse.wst.validation.Validator;
import org.junit.Rule;
import org.junit.Test;

public class XmlValidatorTest {

  private static final String XML_NO_BANNED_ELEMENTS = "<test></test>";
  private static final String XML =
      "<application xmlns='http://appengine.google.com/ns/1.0'></application>";
  private static final String BAD_XML = "<";
  private static final String APPLICATION_MARKER =
      "com.google.cloud.tools.eclipse.appengine.validation.appEngineBlacklistMarker";

  @Rule public TestProjectCreator appEngineStandardProjectCreator =
      new TestProjectCreator().withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
          AppEngineStandardFacet.JRE7);

  @Rule public TestProjectCreator dynamicWebProjectCreator =
      new TestProjectCreator().withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25);

  @Rule public TestProjectCreator projectCreator = new TestProjectCreator();

  @Test
  public void testValidate_appEngineStandard() {
    IProject project = appEngineStandardProjectCreator.getProject();
    assertTrue("webXmlValidator should be applied to web.xml", webXmlValidatorApplied(project));
  }

  @Test
  public void testValidate_dynamicWebProject() {
    IProject project = dynamicWebProjectCreator.getProject();
    assertFalse("webXmlValidator should not be applied in jst.web project",
        webXmlValidatorApplied(project));
  }

  private static boolean webXmlValidatorApplied(IProject project) {
    IFile webXml = project.getFile("web.xml");
    ValidationFramework framework = ValidationFramework.getDefault();
    Validator[] validators = framework.getValidatorsFor(webXml);
    for (Validator validator : validators) {
      if ("com.google.cloud.tools.eclipse.appengine.validation.webXmlValidator"
          .equals(validator.getId())) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void testValidate_badXml() throws IOException, CoreException {
    XmlValidator validator = new XmlValidator();
    validator.setHelper(new AppEngineWebXmlValidator());

    // This method should not apply any markers for malformed XML
    IFile file = createBogusProjectFile();
    byte[] badXml = BAD_XML.getBytes(StandardCharsets.UTF_8);
    validator.validate(file, badXml);

    IMarker[] emptyMarkers = ProjectUtils.waitUntilNoMarkersFound(file, IMarker.PROBLEM,
        true /* includeSubtypes */, IResource.DEPTH_ZERO);
    ArrayAssertions.assertIsEmpty(emptyMarkers);
  }

  @Test
  public void testValidate_badXml_dynamicWebProject() throws CoreException {
    IProject dynamicWebProject = dynamicWebProjectCreator.getProject();
    IFile file = dynamicWebProject.getFile("src/bad.xml");
    byte[] badXml = BAD_XML.getBytes(StandardCharsets.UTF_8);
    file.create(new ByteArrayInputStream(badXml), true, null);

    IMarker[] markers = ProjectUtils.waitUntilMarkersFound(file, IMarker.PROBLEM,
        true /* includeSubtypes */, IResource.DEPTH_ZERO);
    ArrayAssertions.assertSize(1, markers);

    String resultMessage = (String) markers[0].getAttribute(IMarker.MESSAGE);
    assertEquals("XML document structures must start and end within the same entity.",
        resultMessage);
  }

  @Test
  public void testValidate_noBannedElements() throws IOException, CoreException {
    XmlValidator validator = new XmlValidator();
    validator.setHelper(new AppEngineWebXmlValidator());

    IFile file = createBogusProjectFile();
    byte[] bytes = XML_NO_BANNED_ELEMENTS.getBytes(StandardCharsets.UTF_8);
    validator.validate(file, bytes);

    IMarker[] markers = file.findMarkers(APPLICATION_MARKER, true, IResource.DEPTH_ZERO);
    ArrayAssertions.assertIsEmpty(markers);
  }

  @Test
  public void testValidate_withBannedElements() throws IOException, CoreException {
    XmlValidator validator = new XmlValidator();
    validator.setHelper(new AppEngineWebXmlValidator());

    IFile file = createBogusProjectFile();
    byte[] bytes = XML.getBytes(StandardCharsets.UTF_8);
    validator.validate(file, bytes);

    IMarker[] markers = file.findMarkers(APPLICATION_MARKER, true, IResource.DEPTH_ZERO);
    ArrayAssertions.assertSize(1, markers);
    String message = Messages.getString("application.element");
    assertEquals(message, markers[0].getAttribute(IMarker.MESSAGE));
    assertEquals("line 1", markers[0].getAttribute(IMarker.LOCATION));
  }

  @Test
  public void testXsdValidation_appengineWebXml() throws CoreException {
    String xml = "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
        + "<foo></foo>"
        + "</appengine-web-app>";
    IProject project = appEngineStandardProjectCreator.getProject();
    IFile file = project.getFile("WebContent/WEB-INF/appengine-web.xml");
    file.setContents(
        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), true, false, null);
    ProjectUtils.waitForProjects(project);  // Wait until Eclipse puts an error marker.

    String problemMarker = "org.eclipse.core.resources.problemmarker";
    IMarker[] markers = file.findMarkers(problemMarker, true, IResource.DEPTH_ZERO);
    ArrayAssertions.assertSize(1, markers);
    assertTrue(markers[0].getAttribute(IMarker.MESSAGE).toString().contains(
        "Invalid content was found starting with element 'foo'."));
  }

  @Test
  public void testCreateMarker() throws CoreException {
    IFile file = createBogusProjectFile();
    String message = "Project ID should be specified at deploy time.";
    BannedElement element = new BannedElement(message);
    XmlValidator.createMarker(file, element);
    IMarker[] markers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
    ArrayAssertions.assertSize(1, markers);
    assertEquals(message, markers[0].getAttribute(IMarker.MESSAGE));
  }

  private IFile createBogusProjectFile() throws CoreException {
    IProject project = projectCreator.getProject();
    IFile file = project.getFile("bogus.resource.for.marker.tests");
    file.create(new ByteArrayInputStream(new byte[0]), true, null);
    return file;
  }
}
