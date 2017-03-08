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

public class AbstractXmlValidatorTest {

  private final String ELEMENT_MESSAGE = "Project ID should be specified at deploy time.";
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
  public void testCreateMarker() throws CoreException {
    BannedElement element = new BannedElement(ELEMENT_MESSAGE);
    AppEngineWebXmlValidator.createMarker(resource, element, 0);
    IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
    assertEquals(ELEMENT_MESSAGE, markers[0].getAttribute(IMarker.MESSAGE));
  }

  @Test
  public void testCreateSAXErrorMessage() throws CoreException {
    SAXParseException spe = new SAXParseException("", "", "", 1, 1);
    SAXException ex = new SAXException(ELEMENT_MESSAGE, spe);
    AppEngineWebXmlValidator.createSaxErrorMessage(resource, ex);
    IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
    assertEquals(ELEMENT_MESSAGE, markers[0].getAttribute(IMarker.MESSAGE));
  }

  private static void createFolders(IContainer parent, IPath path) throws CoreException {
    if (!path.isEmpty()) {
      IFolder folder = parent.getFolder(new Path(path.segment(0)));
      folder.create(true,  true,  null);
      createFolders(folder, path.removeFirstSegments(1));
    }
  }

}
