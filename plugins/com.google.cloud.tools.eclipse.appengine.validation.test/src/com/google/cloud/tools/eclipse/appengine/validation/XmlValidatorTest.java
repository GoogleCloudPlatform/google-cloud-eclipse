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
import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class XmlValidatorTest {

  private static final String XML_NO_BANNED_ELEMENTS = "<test></test>";
  private static final String XML = "<application></application>";
  private static final String BAD_XML = "<";
  private static final String APPLICATION_MARKER =
      "com.google.cloud.tools.eclipse.appengine.validation.appEngineBlacklistMarker";
  private IFile resource;

  @Rule public TestProjectCreator appEngineStandardProjectCreator =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25,
          AppEngineStandardFacet.FACET_VERSION);

  @Rule public TestProjectCreator dynamicWebProjectCreator =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25);

  @Before
  public void setUp() {
    IProject project = dynamicWebProjectCreator.getProject();
    resource = project.getFile("WebContent/WEB-INF/web.xml");
  }

  @Test
  public void testValidate_appEngineStandard() {
    IProject project = appEngineStandardProjectCreator.getProject();
    IFile file = project.getFile("src/main/webapp/WEB-INF/web.xml");
    ValidationFramework framework = ValidationFramework.getDefault();
    Validator[] validators = framework.getValidatorsFor(file);
    for (Validator validator : validators) {
      if ("com.google.cloud.tools.eclipse.appengine.validation.webXmlValidator"
          .equals(validator.getId())) {
        return;
      }
    }
    fail("webXmlValidator isn't applied to web.xml");
  }

  @Test
  public void testValidate_dynamicWebProject() {
    ValidationFramework framework = ValidationFramework.getDefault();
    Validator[] validators = framework.getValidatorsFor(resource);
    for (Validator validator : validators) {
      if ("com.google.cloud.tools.eclipse.appengine.validation.webXmlValidator"
          .equals(validator.getId())) {
        fail("webXmlValidator should not be applied in jst.web project");
      }
    }
  }

  @Test
  public void testValidate_badXml() throws IOException, CoreException {
    IProject project = dynamicWebProjectCreator.getProject();
    IFile file = project.getFile("src/test");
    byte[] bytes = BAD_XML.getBytes(StandardCharsets.UTF_8);
    file.create(new ByteArrayInputStream(bytes), true, null);

    XmlValidator validator = new XmlValidator();
    validator.setHelper(new AppEngineWebXmlValidator());

    // This method should not apply any markers for malformed XML
    validator.validate(file, bytes);
    IMarker[] emptyMarkers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
    assertEquals(0, emptyMarkers.length);

    // This method should apply markers for malformed XML
    validator.xsdValidation(file);
    IMarker[] markers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
    assertEquals(1, markers.length);

    String resultMessage = (String) markers[0].getAttribute(IMarker.MESSAGE);
    assertEquals("XML document structures must start and end within the same entity.",
        resultMessage);
    resource.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
  }

  @Test
  public void testValidate_noBannedElements() throws IOException, CoreException {
    byte[] bytes = XML_NO_BANNED_ELEMENTS.getBytes(StandardCharsets.UTF_8);
    XmlValidator validator = new XmlValidator();
    validator.setHelper(new AppEngineWebXmlValidator());
    validator.validate(resource, bytes);
    IMarker[] markers = resource.findMarkers(APPLICATION_MARKER, true, IResource.DEPTH_ZERO);
    assertEquals(0, markers.length);
  }

  @Test
  public void testValidate_withBannedElements() throws IOException, CoreException {
    byte[] bytes = XML.getBytes(StandardCharsets.UTF_8);
    XmlValidator validator = new XmlValidator();
    validator.setHelper(new AppEngineWebXmlValidator());
    validator.validate(resource, bytes);
    IMarker[] markers = resource.findMarkers(APPLICATION_MARKER, true, IResource.DEPTH_ZERO);
    assertEquals(1, markers.length);
    String message = Messages.getString("application.element");
    assertEquals(message, markers[0].getAttribute(IMarker.MESSAGE));
    assertEquals("line 1", markers[0].getAttribute(IMarker.LOCATION));
    resource.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
  }

  @Test
  public void testXsdValidation() throws CoreException {
    String xml = "<appengine-web-app xmlns='http://appengine.google.com/ns/1.0'>"
        + "<foo></foo>"
        + "</appengine-web-app>";
    XmlValidator validator = new XmlValidator();
    validator.setHelper(new AppEngineWebXmlValidator());
    IProject project = appEngineStandardProjectCreator.getProject();
    ResourceUtils.createFolders(project.getFolder("src/main/webapp/WEB-INF"), null);
    IFile file = project.getFile("src/main/webapp/WEB-INF/appengine-web.xml");
    file.create(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), true, null);
    validator.xsdValidation(file);
    String problemMarker = "org.eclipse.core.resources.problemmarker";
    IMarker[] markers = file.findMarkers(problemMarker, true, IResource.DEPTH_ZERO);
    StringBuilder builder = new StringBuilder();
    for (IMarker marker : markers) {
      builder.append(marker.getAttribute(IMarker.MESSAGE) + "\n");
    }
    String message = String.format("Expected 1 marker, got %d markers with messages: %s",
        markers.length, builder.toString());
    assertEquals(message, 1, markers.length);
  }

  @Test
  public void testCreateMarker() throws CoreException {
    String message = "Project ID should be specified at deploy time.";
    BannedElement element = new BannedElement(message);
    XmlValidator.createMarker(resource, element);
    IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
    assertEquals(message, markers[0].getAttribute(IMarker.MESSAGE));
    resource.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
  }

}
