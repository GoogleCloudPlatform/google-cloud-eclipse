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
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;

public class PomXmlValidatorTest {

  private static final String PROJECT_START_TAG = "<project xmlns='http://maven.apache.org/POM/4.0.0' "
      + "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
      + "xsi:schemaLocation='http://maven.apache.org/POM/4.0.0 "
      + "http://maven.apache.org/xsd/maven-4.0.0.xsd'>";
  private static final String XML_NO_BANNED_ELEMENTS = PROJECT_START_TAG + "<test></test></project>";
  private static final String XML = PROJECT_START_TAG
      + "<build><plugins><plugin><groupId>com.google.appengine</groupId>"
      + "<artifactId>appengine-maven-plugin</artifactId></plugin></plugins></build></project>";
  private static final String PLUGIN_MARKER =
      "com.google.cloud.tools.eclipse.appengine.validation.mavenPluginMarker";
  private static IResource resource;
  private static IProject project;
  
  @ClassRule public static TestProjectCreator projectCreator = new TestProjectCreator();
  
  @BeforeClass
  public static void setUp() throws CoreException {
    project = projectCreator.getProject();
    createFolders(project, new Path("src/main/webapp/WEB-INF"));
    resource = project.getFile("src/main/webapp/WEB-INF/web.xml");
    ((IFile) resource).create(new ByteArrayInputStream(new byte[0]), true, null);
  }
  
  
  @After
  public void tearDown() throws CoreException {
    if (resource != null) {
      resource.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
    }
  }
  
  @Test
  public void testValidate_noBannedElements()
      throws IOException, CoreException, ParserConfigurationException {
    byte[] bytes = XML_NO_BANNED_ELEMENTS.getBytes(StandardCharsets.UTF_8);
    PomXmlValidator validator = new PomXmlValidator();
    validator.validate(resource, bytes);
    IMarker[] markers = resource.findMarkers(PLUGIN_MARKER, true, IResource.DEPTH_ZERO);
    assertEquals(0, markers.length);
  }

  @Test
  public void testValidate_withBannedElements()
      throws IOException, CoreException, ParserConfigurationException {
    byte[] bytes = XML.getBytes(StandardCharsets.UTF_8);
    PomXmlValidator validator = new PomXmlValidator();
    validator.validate(resource, bytes);
    IMarker[] markers = resource.findMarkers(PLUGIN_MARKER, true, IResource.DEPTH_ZERO);
    assertEquals(1, markers.length);
    String message = Messages.getString("maven.plugin");
    assertEquals(message, (String) markers[0].getAttribute(IMarker.MESSAGE));
    assertEquals("line 1", markers[0].getAttribute(IMarker.LOCATION));
  }
  
  private static void createFolders(IContainer parent, IPath path) throws CoreException {
    if (!path.isEmpty()) {
      IFolder folder = parent.getFolder(new Path(path.segment(0)));
      folder.create(true,  true,  null);
      createFolders(folder, path.removeFirstSegments(1));
    }
  }
}
