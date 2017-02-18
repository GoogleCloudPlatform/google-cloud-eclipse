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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;

public class AppEngineWebXmlValidatorTest {

  private static final String XML_NO_BANNED_ELEMENTS = "<test></test>";
  private static final String XML = "<application></application>";
  private static final String BAD_XML = "<";
  private static final String ELEMENT_NAME = "application";
  private static final String ELEMENT_MESSAGE = "Project ID should be specified at deploy time";
  private static final String MARKER_TYPE = "org.eclipse.core.resources.problemmarker";
  private static IResource resource;
  private static IProject project;
  
  @ClassRule public static TestProjectCreator projectCreator = new TestProjectCreator();
  
  @BeforeClass
  public static void setUp() throws CoreException {
    project = projectCreator.getProject();
    createFolders(project, new Path("src/main/webapp/WEB-INF"));
    IFile webXml = project.getFile("src/main/webapp/WEB-INF/web.xml");
    webXml.create(new ByteArrayInputStream(new byte[0]), true, null);
    resource = webXml;
  }
  
  @After
  public void tearDown() throws CoreException {
    if (resource != null) {
      resource.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
    }
  }
  
  @Test
  public void testValidate_badXml()
      throws IOException, CoreException, ParserConfigurationException {
    byte[] bytes = BAD_XML.getBytes(StandardCharsets.UTF_8);
    AppEngineWebXmlValidator.validate(resource, bytes);
    IMarker[] markers = resource.findMarkers(MARKER_TYPE, true, IResource.DEPTH_ZERO);
    String resultMessage = (String) markers[0].getAttribute(IMarker.MESSAGE);
    assertEquals("XML document structures must start and end within the same entity.",
        resultMessage);
  }
  
  @Test
  public void testValidate_noBannedTags()
      throws IOException, CoreException, ParserConfigurationException {
    byte[] bytes = XML_NO_BANNED_ELEMENTS.getBytes(StandardCharsets.UTF_8);
    AppEngineWebXmlValidator.validate(resource, bytes);
    IMarker[] markers = resource.findMarkers(MARKER_TYPE, true, IResource.DEPTH_ZERO);
    assertEquals(0, markers.length);
  }

  @Test
  public void testValidate()
      throws IOException, CoreException, ParserConfigurationException {
    byte[] bytes = XML.getBytes(StandardCharsets.UTF_8);
    AppEngineWebXmlValidator.validate(resource, bytes);
    IMarker[] markers = resource.findMarkers(MARKER_TYPE, true, IResource.DEPTH_ZERO);
    assertEquals(1, markers.length);
    assertEquals(ELEMENT_MESSAGE, (String) markers[0].getAttribute(IMarker.MESSAGE));
    assertEquals("line 1", markers[0].getAttribute(IMarker.LOCATION));
  }
  
  @Test
  public void testCreateMarker() throws CoreException {
    BannedElement element = new BannedElement(ELEMENT_NAME);
    AppEngineWebXmlValidator.createMarker(resource, element, 0);
    IMarker[] markers = resource.findMarkers(MARKER_TYPE, true, IResource.DEPTH_ZERO);
    assertEquals(ELEMENT_MESSAGE, (String) markers[0].getAttribute(IMarker.MESSAGE));
  }
  
  @Test
  public void testCreateSAXErrorMessage() throws CoreException {
    SAXParseException spe = new SAXParseException("", "", "", 1, 1);
    SAXException ex = new SAXException(ELEMENT_MESSAGE, spe);
    AppEngineWebXmlValidator.createSaxErrorMessage(resource, ex);
    IMarker[] markers = resource.findMarkers(MARKER_TYPE, true, IResource.DEPTH_ZERO);
    assertEquals(ELEMENT_MESSAGE, (String) markers[0].getAttribute(IMarker.MESSAGE));
  }
  
  private static void createFolders(IContainer parent, IPath path) throws CoreException {
    if (!path.isEmpty()) {
      IFolder folder = parent.getFolder(new Path(path.segment(0)));
      folder.create(true,  true,  null);
      createFolders(folder, path.removeFirstSegments(1));
    }
  }
}
