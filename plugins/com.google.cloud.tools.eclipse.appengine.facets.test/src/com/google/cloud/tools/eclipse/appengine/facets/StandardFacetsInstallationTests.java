/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.facets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.util.FacetedProjectHelper;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.util.internal.ZipUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Tests some cases of installing the App Engine Standard facet on existing projects.
 */
public class StandardFacetsInstallationTests {
  private IWorkspaceRoot root;
  private IProject project;

  @Before
  public void setUp() {
    root = ResourcesPlugin.getWorkspace().getRoot();
  }

  @After
  public void tearDown() throws CoreException {
    if (project != null) {
      project.delete(true, null);
    }
  }

  @Test
  public void testStandardFacetInstallationOnGwtWar() throws IOException, CoreException {
    project = importProject("projects/test-dynamic-web-project.zip");
    IFacetedProject facetedProject = new FacetedProjectHelper().getFacetedProject(project);
    // verify that the appengine-web.xml is installed in the dynamic web root folder
    AppEngineStandardFacet.installAppEngineFacet(facetedProject, true, null);
    IFile correctAppEngineWebXml = project.getFile(new Path("war/WEB-INF/appengine-web.xml"));
    IFile wrongAppEngineWebXml =
        project.getFile(new Path("src/main/webapp/WEB-INF/appengine-web.xml"));
    assertTrue(correctAppEngineWebXml.exists());
    assertFalse(wrongAppEngineWebXml.exists());
  }

  private IProject importProject(String relativeLocation) throws IOException, CoreException {
    SubMonitor progress = SubMonitor.convert(null, 13);
    // Resolve the zip'd project within this bundle
    Bundle bundle = FrameworkUtil.getBundle(getClass());
    URL bundleLocation = bundle.getResource(relativeLocation);
    assertNotNull(bundleLocation);
    URL zipLocation = FileLocator.toFileURL(bundleLocation);
    if (!zipLocation.getProtocol().equals("file")) {
      throw new IOException("could not resolve location to a file");
    }
    File zippedFile = new File(zipLocation.getPath());
    assertTrue(zippedFile.exists());
    progress.worked(1);

    // extract project into our workspace using WTP internal utility class
    ZipUtil.unzip(zippedFile, root.getLocation().toFile(), progress.newChild(2));

    // determine the project location; a bit of a hack as we assume the
    // first entry is the directory, but which seems to hold for all zips
    String dirRoot;
    try (ZipFile zip = new ZipFile(zippedFile)) {
      dirRoot = zip.entries().nextElement().getName();
    }
    // import the project
    IPath projectFileLocation = root.getLocation().append(new Path(dirRoot + ".project"));
    IProjectDescription descriptor =
        root.getWorkspace().loadProjectDescription(projectFileLocation);
    IProject project = root.getProject(descriptor.getName());
    project.create(descriptor, progress.newChild(5)); // adds the project to the workspace
    project.open(progress.newChild(5)); // opens the project
    return project;
  }
}
