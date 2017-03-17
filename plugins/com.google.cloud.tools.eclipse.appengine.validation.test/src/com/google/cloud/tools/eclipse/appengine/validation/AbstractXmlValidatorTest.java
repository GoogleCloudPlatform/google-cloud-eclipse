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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.validation.ValidationFramework;
import org.eclipse.wst.validation.Validator;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class AbstractXmlValidatorTest {

  private static final IProjectFacetVersion APPENGINE_STANDARD_FACET_VERSION_1 =
      ProjectFacetsManager.getProjectFacet(AppEngineStandardFacet.ID).getVersion("1");
  private IFile resource;

  @ClassRule public static TestProjectCreator appEngineStandardProjectCreator =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7,
          WebFacetUtils.WEB_25, APPENGINE_STANDARD_FACET_VERSION_1);
  
  @ClassRule public static TestProjectCreator dynamicWebProjectCreator =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25);

  @Before
  public void setUp() throws CoreException {
    IProject project = dynamicWebProjectCreator.getProject();
    resource = project.getFile("WebContent/WEB-INF/web.xml");
  }
  
  @Test
  public void testValidate_appEngineStandard() throws CoreException {
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
  public void testValidate_dynamicWebProject() throws CoreException {
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
  public void testCreateMarker() throws CoreException {
    String message = "Project ID should be specified at deploy time.";
    BannedElement element = new BannedElement(message);
    AppEngineWebXmlValidator.createMarker(resource, element, 0);
    IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
    assertEquals(message, markers[0].getAttribute(IMarker.MESSAGE));
  }

  @Test
  public void testCreateSAXErrorMessage() throws CoreException {
    String message = "Project ID should be specified at deploy time.";
    SAXParseException spe = new SAXParseException("", "", "", 1, 1);
    SAXException ex = new SAXException(message, spe);
    AppEngineWebXmlValidator.createSaxErrorMessage(resource, ex);
    IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
    assertEquals(message, markers[0].getAttribute(IMarker.MESSAGE));
  }

}
