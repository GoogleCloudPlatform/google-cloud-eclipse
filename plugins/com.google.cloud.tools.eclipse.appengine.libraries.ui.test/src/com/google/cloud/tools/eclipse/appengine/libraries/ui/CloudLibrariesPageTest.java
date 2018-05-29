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

package com.google.cloud.tools.eclipse.appengine.libraries.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.libraries.LibraryClasspathContainer;
import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class CloudLibrariesPageTest {
  private static final IPath MASTER_CONTAINER_PATH =
      new Path(LibraryClasspathContainer.CONTAINER_PATH_PREFIX)
          .append(CloudLibraries.MASTER_CONTAINER_ID);

  private final CloudLibrariesPage page = new CloudLibrariesPage();

  @Rule
  public ShellTestResource shellTestResource = new ShellTestResource();

  @Rule
  public TestProjectCreator plainJavaProjectCreator =
      new TestProjectCreator().withFacets(JavaFacet.VERSION_1_7);

  @Test
  public void testConstructor() {
    Assert.assertEquals("Google Cloud Platform Libraries", page.getTitle());
    Assert.assertNull(page.getMessage());
    Assert.assertNull(page.getErrorMessage());
    Assert.assertEquals("Additional jars for applications using Google Cloud Platform",
        page.getDescription());
    Assert.assertNotNull(page.getImage());
  }

  @Test
  public void testSetSelection_null() {
    // creates a new container
    IJavaProject javaProject = plainJavaProjectCreator.getJavaProject();
    page.initialize(javaProject, null);
    page.setSelection(null);
  }

  @Test
  public void testGetSelection() {
    // a new page with no library selections shouldn't bother creating a new container
    Assert.assertNull(page.getSelection());
  }

  @Test
  public void testSelectionRoundTrip() {
    IJavaProject javaProject = plainJavaProjectCreator.getJavaProject();
    List<Library> selectedLibraries =
        Arrays.asList(CloudLibraries.getLibrary("googlecloudstorage"));
    page.initialize(javaProject, null);
    page.setSelectedLibraries(selectedLibraries);
    page.createControl(shellTestResource.getShell());

    List<Library> returnedLibraries = page.getSelectedLibraries();
    Assert.assertEquals(1, returnedLibraries.size());
    Assert.assertEquals("googlecloudstorage", returnedLibraries.get(0).getId());
  }

  @Test
  public void testAppEngineLibraries_foundOnAppEngineStandardProject() {
    IJavaProject javaProject = plainJavaProjectCreator
        .withFacets(WebFacetUtils.WEB_25, AppEngineStandardFacet.JRE7).getJavaProject();
    page.initialize(javaProject, null);
    page.createControl(shellTestResource.getShell());
    assertThat(page.libraryGroups, Matchers.hasKey(CloudLibraries.APP_ENGINE_STANDARD_GROUP));
  }

  @Test
  public void testAppEngineLibraries_missingOnPlainJavaProject() {
    IJavaProject javaProject = plainJavaProjectCreator.getJavaProject();
    page.initialize(javaProject, null);
    page.createControl(shellTestResource.getShell());
    assertThat(page.libraryGroups, 
        Matchers.not(Matchers.hasKey(CloudLibraries.APP_ENGINE_STANDARD_GROUP)));
  }

  @Test
  public void testSelectionMaintained() {
    // explicitly configure App Engine and GCP libraries
    IJavaProject javaProject = plainJavaProjectCreator.getJavaProject();
    LinkedHashMap<String, String> groups = new LinkedHashMap<>();
    groups.put(CloudLibraries.APP_ENGINE_STANDARD_GROUP, CloudLibraries.APP_ENGINE_STANDARD_GROUP);
    groups.put(CloudLibraries.CLIENT_APIS_GROUP, CloudLibraries.CLIENT_APIS_GROUP);

    // select objectify
    page.setSelectedLibraries(Collections.singletonList(CloudLibraries.getLibrary("objectify"))); //$NON-NLS-1$
    page.initialize(javaProject, null);
    page.setLibraryGroups(groups);
    page.createControl(shellTestResource.getShell());
    assertTrue(page.librariesSelectors != null && !page.librariesSelectors.isEmpty());
    assertEquals(2, page.librariesSelectors.size());

    // check the library group widgets
    assertThat(page.librariesSelectors.get(0).getSelectedLibraries(),
        Matchers.hasItem(new LibraryMatcher("objectify")));
    assertThat(page.librariesSelectors.get(1).getSelectedLibraries(), Matchers.empty());

    // check the page's selected libraries
    List<Library> returnedLibraries = page.getSelectedLibraries();
    Assert.assertEquals(2, returnedLibraries.size());
    assertThat(returnedLibraries, Matchers.hasItem(new LibraryMatcher("objectify")));

    // select GCS
    page.librariesSelectors.get(1).setSelection(new StructuredSelection("googlecloudstorage"));

    // check the library group widgets
    assertThat(page.librariesSelectors.get(0).getSelectedLibraries(),
        Matchers.hasItem(new LibraryMatcher("objectify")));
    assertThat(page.librariesSelectors.get(1).getSelectedLibraries(),
        Matchers.hasItem(new LibraryMatcher("googlecloudstorage")));

    returnedLibraries = page.getSelectedLibraries();
    Assert.assertEquals(3, returnedLibraries.size());
    assertThat(returnedLibraries, Matchers.hasItem(new LibraryMatcher("appengine-api")));
    assertThat(returnedLibraries, Matchers.hasItem(new LibraryMatcher("objectify")));
    assertThat(returnedLibraries, Matchers.hasItem(new LibraryMatcher("googlecloudstorage")));

    // unselect objectify
    page.librariesSelectors.get(0).setSelection(StructuredSelection.EMPTY);

    // check the library group widgets
    assertThat(page.librariesSelectors.get(0).getSelectedLibraries(), Matchers.empty());
    assertThat(page.librariesSelectors.get(1).getSelectedLibraries(),
        Matchers.hasItem(new LibraryMatcher("googlecloudstorage")));

    returnedLibraries = page.getSelectedLibraries();
    Assert.assertEquals(1, returnedLibraries.size());
    assertThat(returnedLibraries, Matchers.hasItem(new LibraryMatcher("googlecloudstorage")));
  }

  @Test
  public void testPreventMultipleContainers() {
    IClasspathEntry existingEntry = mock(IClasspathEntry.class);
    when(existingEntry.getEntryKind()).thenReturn(IClasspathEntry.CPE_CONTAINER);
    when(existingEntry.getPath()).thenReturn(MASTER_CONTAINER_PATH);
    assertTrue(LibraryClasspathContainer.isEntry(existingEntry));

    // explicitly configure App Engine and GCP libraries
    IJavaProject javaProject = plainJavaProjectCreator.getJavaProject();

    page.initialize(javaProject, new IClasspathEntry[] {existingEntry});
    page.setSelection(null);
    page.createControl(shellTestResource.getShell());

    assertTrue(page.finish()); // should be ok
    assertNull(page.getSelection()); // no new container created
  }

  private static class LibraryMatcher extends BaseMatcher<Library> {
    private String libraryId;

    private LibraryMatcher(String libraryId) {
      this.libraryId = libraryId;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("Looking for " + libraryId);
    }

    @Override
    public boolean matches(Object item) {
      return item instanceof Library && libraryId.equals(((Library) item).getId());
    }
  }
}
