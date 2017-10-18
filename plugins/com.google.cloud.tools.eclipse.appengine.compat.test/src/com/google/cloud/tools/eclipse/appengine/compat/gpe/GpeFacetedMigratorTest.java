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

package com.google.cloud.tools.eclipse.appengine.compat.gpe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.appengine.compat.gpe.GpeMigrator;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import java.io.IOException;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Migrates a faceted GPE project. */
public class GpeFacetedMigratorTest {

  private final IProgressMonitor monitor = new NullProgressMonitor();

  private IProject gpeProject;

  @Before
  public void setUp() throws IOException, CoreException {
    List<IProject> projects = ProjectUtils.importProjects(getClass(),
        "test-projects/GPE-faceted-project.zip", false /* checkBuildErrors */, monitor);
    assertEquals(1, projects.size());
    gpeProject = projects.get(0);
  }

  @After
  public void tearDown() throws CoreException {
    gpeProject.delete(true /* force */,  monitor);
  }

  @Test
  public void testRemoveGpeNature() throws CoreException {
    assertTrue(gpeProject.hasNature("com.google.appengine.eclipse.core.gaeNature"));

    assertTrue(GpeMigrator.removeGpeNature(gpeProject, monitor));
    assertFalse(gpeProject.hasNature("com.google.appengine.eclipse.core.gaeNature"));

    assertFalse(GpeMigrator.removeGpeNature(gpeProject, monitor));
  }

  @Test
  public void testRemoveGpeRuntimeAndFacets_facetsRemoved() throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(gpeProject);

    assertTrue(containsFacet(facetedProject, "com.google.appengine.facet"));
    assertTrue(containsFacet(facetedProject, "com.google.appengine.facet.ear"));

    assertTrue(GpeMigrator.removeGpeRuntimeAndFacets(facetedProject));
    assertFalse(containsFacet(facetedProject, "com.google.appengine.facet"));
    assertFalse(containsFacet(facetedProject, "com.google.appengine.facet.ear"));

    assertFalse(GpeMigrator.removeGpeRuntimeAndFacets(facetedProject));
  }

  @Test
  public void testRemoveGpeRuntimeAndFacets_runtimeRemoved() throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(gpeProject);

    assertEquals(1, facetedProject.getTargetedRuntimes().size());
    assertEquals("Google App Engine", facetedProject.getPrimaryRuntime().getName());
    assertEquals("Google App Engine",
        facetedProject.getTargetedRuntimes().iterator().next().getName());

    assertTrue(GpeMigrator.removeGpeRuntimeAndFacets(facetedProject));
    assertNull(facetedProject.getPrimaryRuntime());
    assertTrue(facetedProject.getTargetedRuntimes().isEmpty());

    assertFalse(GpeMigrator.removeGpeRuntimeAndFacets(facetedProject));
  }

  @Test
  public void testRemoveGpeRuntimeAndFacets_metadataFileDoesNotExist() throws CoreException {
    IFile metadataFile =
        gpeProject.getFile(".settings/org.eclipse.wst.common.project.facet.core.xml");
    metadataFile.delete(true, monitor);
    assertFalse(metadataFile.exists());

    IFacetedProject facetedProject = ProjectFacetsManager.create(gpeProject);
    assertFalse(GpeMigrator.removeGpeRuntimeAndFacets(facetedProject));
  }

  @Test
  public void testRemoveGpeClasspathEntries() throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(gpeProject);

    assertTrue(containsLibrary(javaProject, "com.google.appengine.eclipse.core.GAE_CONTAINER"));
    assertTrue(containsLibrary(javaProject, "com.google.appengine.eclipse.wtp.GAE_WTP_CONTAINER"));
    assertTrue(containsLibrary(javaProject, "org.eclipse.jst.server.core.container/"
        + "com.google.appengine.server.runtimeTarget/Google App Engine"));
    assertTrue(containsLibrary(javaProject,
        "com.google.gdt.eclipse.managedapis.MANAGED_API_CONTAINER/compute-v1r150lv1.21.0"));

    assertTrue(GpeMigrator.removeGpeClasspathEntries(gpeProject, monitor));
    assertFalse(containsLibrary(javaProject, "com.google.appengine.eclipse.core.GAE_CONTAINER"));
    assertFalse(containsLibrary(javaProject, "com.google.appengine.eclipse.wtp.GAE_WTP_CONTAINER"));
    assertFalse(containsLibrary(javaProject, "org.eclipse.jst.server.core.container/"
        + "com.google.appengine.server.runtimeTarget/Google App Engine"));
    assertFalse(containsLibrary(javaProject,
        "com.google.gdt.eclipse.managedapis.MANAGED_API_CONTAINER/compute-v1r150lv1.21.0"));

    assertFalse(GpeMigrator.removeGpeClasspathEntries(gpeProject, monitor));
  }

  private static boolean containsFacet(IFacetedProject facetedProject, String facetId) {
    for (IProjectFacetVersion facet : facetedProject.getProjectFacets()) {
      if (facet.getProjectFacet().getId().equals(facetId)) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsLibrary(IJavaProject javaProject, String libraryPath)
      throws JavaModelException {
    for (IClasspathEntry entry : javaProject.getRawClasspath()) {
      if (entry.getPath().equals(new Path(libraryPath))) {
        return true;
      }
    }
    return false;
  }
}
