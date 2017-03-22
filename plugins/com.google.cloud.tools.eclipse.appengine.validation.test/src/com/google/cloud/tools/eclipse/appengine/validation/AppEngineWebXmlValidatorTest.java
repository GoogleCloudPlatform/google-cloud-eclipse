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

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class AppEngineWebXmlValidatorTest {

  private static final String XML_NO_BANNED_ELEMENTS = "<test></test>";
  private static final String XML = "<application></application>";
  private static final String BAD_XML = "<";
  private static final String APPLICATION_MARKER =
      "com.google.cloud.tools.eclipse.appengine.validation.appEngineBlacklistMarker";
  private IFile webXmlFile;
  private IProject project;

  @ClassRule public static TestProjectCreator projectCreator =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25);

  @Before
  public void setUp() throws CoreException {
    project = projectCreator.getProject();
    ValidationTestUtils.createFolders(project, new Path("src/main/webapp/WEB-INF"));
    webXmlFile = project.getFile("src/main/webapp/WEB-INF/web.xml");
    webXmlFile.create(new ByteArrayInputStream(new byte[0]), true, null);
  }
  
  @After
  public void tearDown() throws CoreException {
    webXmlFile.delete(true, null);
  }

  @Test
  public void testValidate_badXml()
      throws IOException, CoreException, ParserConfigurationException {
    byte[] bytes = BAD_XML.getBytes(StandardCharsets.UTF_8);
    AppEngineWebXmlValidator validator = new AppEngineWebXmlValidator();
    validator.validate(webXmlFile, bytes);
    IMarker[] markers = webXmlFile.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
    String resultMessage = (String) markers[0].getAttribute(IMarker.MESSAGE);
    assertEquals("XML document structures must start and end within the same entity.",
        resultMessage);
  }

  @Test
  public void testValidate_noBannedElements()
      throws IOException, CoreException, ParserConfigurationException {
    byte[] bytes = XML_NO_BANNED_ELEMENTS.getBytes(StandardCharsets.UTF_8);
    AppEngineWebXmlValidator validator = new AppEngineWebXmlValidator();
    validator.validate(webXmlFile, bytes);
    IMarker[] markers = webXmlFile.findMarkers(APPLICATION_MARKER, true, IResource.DEPTH_ZERO);
    assertEquals(0, markers.length);
  }

  @Test
  public void testValidate_withBannedElements()
      throws IOException, CoreException, ParserConfigurationException {
    byte[] bytes = XML.getBytes(StandardCharsets.UTF_8);
    AppEngineWebXmlValidator validator = new AppEngineWebXmlValidator();
    validator.validate(webXmlFile, bytes);
    IMarker[] markers = webXmlFile.findMarkers(APPLICATION_MARKER, true, IResource.DEPTH_ZERO);
    assertEquals(1, markers.length);
    String message = Messages.getString("application.element");
    assertEquals(message, markers[0].getAttribute(IMarker.MESSAGE));
    assertEquals("line 1", markers[0].getAttribute(IMarker.LOCATION));
  }
}
