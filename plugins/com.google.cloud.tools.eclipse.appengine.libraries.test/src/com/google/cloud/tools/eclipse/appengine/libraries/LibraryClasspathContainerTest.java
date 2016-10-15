/*******************************************************************************
 * Copyright 2016 Google Inc.
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
 *******************************************************************************/
package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.LibraryRepositoryServiceException;

@RunWith(MockitoJUnitRunner.class)
public class LibraryClasspathContainerTest {

  @Mock private ILibraryRepositoryService repositoryService;
  private ServiceRegistration<ILibraryRepositoryService> serviceRegistration = null;

  @After
  public void tearDown() {
    if (serviceRegistration != null) {
      serviceRegistration.unregister();
    }
  }

  @Test
  public void testGetDescription_returnsNameIfNotNullOrEmpty() {
    Library library = new Library("id");
    library.setName("libraryName");
    LibraryClasspathContainer classpathContainer = new LibraryClasspathContainer(new Path("container/path"), library);
    assertThat(classpathContainer.getDescription(), is("libraryName"));
  }

  @Test
  public void testGetDescription_returnsIdIfNameNull() {
    Library library = new Library("id");
    library.setName(null);
    LibraryClasspathContainer classpathContainer = new LibraryClasspathContainer(new Path("container/path"), library);
    assertThat(classpathContainer.getDescription(), is("id"));
  }

  @Test
  public void testGetDescription_returnsIdIfNameEmpty() {
    Library library = new Library("id");
    library.setName("");
    LibraryClasspathContainer classpathContainer = new LibraryClasspathContainer(new Path("container/path"), library);
    assertThat(classpathContainer.getDescription(), is("id"));
  }

  @Test
  public void testGetKind_returnsApplication() throws Exception {
    LibraryClasspathContainer classpathContainer = new LibraryClasspathContainer(new Path("container/path"),
                                                                                 new Library("id"));
    assertThat(classpathContainer.getKind(), is(IClasspathContainer.K_APPLICATION));
  }

  @Test
  public void testGetClasspathEntries_repoServiceThrowsException() throws LibraryRepositoryServiceException {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    when(repositoryService.getJarLocation(mavenCoordinates))
      .thenThrow(new LibraryRepositoryServiceException("test exception"));
    registerMockService();

    Library library = new Library("id");
    LibraryFile libraryFile = new LibraryFile(mavenCoordinates);
    libraryFile.setFilters(Collections.singletonList(Filter.exclusionFilter("com.example.**")));
    library.setLibraryFiles(Collections.singletonList(libraryFile));

    IClasspathEntry[] classpathEntries =
        new LibraryClasspathContainer(new Path("container/path"), library).getClasspathEntries();
    assertNotNull(classpathEntries);
    assertThat(classpathEntries.length, is(0));
  }

  @Test
  public void testGetClasspathEntries() throws CoreException, LibraryRepositoryServiceException {
    MavenCoordinates mavenCoordinates = new MavenCoordinates("groupId", "artifactId");
    when(repositoryService.getJarLocation(mavenCoordinates)).thenReturn(new Path("/path/to/jar/file.jar"));
    registerMockService();

    Library library = new Library("id");
    LibraryFile exportedLibraryFile = new LibraryFile(mavenCoordinates);
    exportedLibraryFile.setFilters(Collections.singletonList(Filter.exclusionFilter("com.example.**")));
    exportedLibraryFile.setExport(true);
    
    LibraryFile notExportedLibraryFile = new LibraryFile(mavenCoordinates);
    notExportedLibraryFile.setExport(false);
    
    library.setLibraryFiles(Arrays.asList(exportedLibraryFile, notExportedLibraryFile));

    LibraryClasspathContainer classpathContainer = new LibraryClasspathContainer(new Path("container/path"), library);
    IClasspathEntry[] classpathEntries = classpathContainer.getClasspathEntries();

    assertThat(classpathEntries.length, is(2));
    IClasspathEntry classpathEntry = classpathEntries[0];
    assertThat(classpathEntry.getEntryKind(), is(IClasspathEntry.CPE_LIBRARY));
    assertThat(classpathEntry.getPath().toString(), is("/path/to/jar/file.jar"));
    assertClasspathEntry(classpathEntry, UpdateClasspathAttributeUtil.createDependencyAttribute(true /* isWebApp */));
    IAccessRule[] accessRules = classpathEntry.getAccessRules();
    assertNotNull(accessRules);
    assertThat(accessRules.length, is(1));
    IAccessRule accessRule = accessRules[0];
    assertThat(accessRule.getKind(), is(IAccessRule.K_NON_ACCESSIBLE));
    assertThat(accessRule.getPattern().toString(), is("com.example.**"));

    IClasspathEntry classpathEntry2 = classpathEntries[1];
    assertClasspathEntry(classpathEntry2, UpdateClasspathAttributeUtil.createNonDependencyAttribute());
    verify(repositoryService, times(2)).getJarLocation(mavenCoordinates);
  }

  private void assertClasspathEntry(IClasspathEntry classpathEntry, IClasspathAttribute classpathAttribute) {
    IClasspathAttribute[] classpathAttributes = classpathEntry.getExtraAttributes();
    assertNotNull(classpathAttributes);
    assertThat(classpathAttributes.length, is(1));
    assertThat(classpathAttributes[0], is(classpathAttribute));
  }

  private void registerMockService() {
    Hashtable<String, Object> properties = new Hashtable<String, Object>();
    properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
    serviceRegistration = FrameworkUtil.getBundle(LibraryClasspathContainer.class).getBundleContext()
        .registerService(ILibraryRepositoryService.class, repositoryService, properties);
  }
}
