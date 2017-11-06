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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.google.cloud.tools.eclipse.appengine.facets.AppEngineStandardFacet;
import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.test.util.ui.ShellTestResource;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class CloudLibrariesPageTest {
  private final CloudLibrariesPage page = new CloudLibrariesPage();

  @Rule
  public ShellTestResource shellTestResource = new ShellTestResource();

  @Rule
  public TestProjectCreator plainJavaProjectCreator =
      new TestProjectCreator().withFacetVersions(JavaFacet.VERSION_1_7);

  @Test
  public void testConstructor() {
    Assert.assertEquals("Google Client APIs for Java", page.getTitle());
    Assert.assertNull(page.getMessage());
    Assert.assertNull(page.getErrorMessage());
    Assert.assertEquals("Additional jars for applications using Google Client APIs for Java",
        page.getDescription());
    Assert.assertNotNull(page.getImage());
  }

  @Test
  public void testSetSelection_null() {
    // creates a new container
    page.setSelection(null);
  }

  @Test
  public void testGetSelection() {
    // a new page with no library selections shouldn't bother creating a new container
    Assert.assertNull(page.getSelection());
  }

  @Test
  public void testSelectionRoundTrip() {
    List<Library> selectedLibraries = Arrays.asList(new Library("foo"));
    page.setSelectedLibraries(selectedLibraries);

    List<Library> returnedLibraries = page.getSelectedLibraries();
    Assert.assertEquals(1, returnedLibraries.size());
    Assert.assertEquals("foo", returnedLibraries.get(0).getId());
  }

  @Test
  public void testAppEngineLibraries_foundOnAppEngineProject() {
    IJavaProject javaProject = plainJavaProjectCreator.withFacetVersions(WebFacetUtils.WEB_25, AppEngineStandardFacet.JRE7)
        .getJavaProject();
    page.initialize(javaProject, null);
    page.createControl(shellTestResource.getShell());
    assertThat(page.getVisibleLibraries(), Matchers.hasItem(new LibraryMatcher("objectify")));
  }

  @Test
  public void testAppEngineLibraries_missingOnPlainJavaProject() {
    IJavaProject javaProject = plainJavaProjectCreator.getJavaProject();
    page.initialize(javaProject, null);
    page.createControl(shellTestResource.getShell());
    Library objectify = CloudLibraries.getLibrary("objectify");
    assertNotNull(objectify);
    // objectify shouldn't be found on a non-App Engine project
    assertThat(page.getVisibleLibraries(),
        Matchers.everyItem(Matchers.not(new LibraryMatcher("objectify"))));
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
