/*
 * Copyright 2017 Google Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.componentcore.resources.IVirtualFile;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class WebProjectUtilTest {
  private static final Logger logger = Logger.getLogger(WebProjectUtilTest.class.getName());

  @Rule
  public TestProjectCreator testProjectCreator = new TestProjectCreator();

  private IProject importedProject;

  @After
  public void tearDown() {
    if (importedProject != null) {
      try {
        importedProject.delete(true, null);
      } catch (CoreException ex) {
        logger.log(Level.SEVERE, "Failure removing imported project", ex);
      }
    }
  }

  @Test
  public void testFindWebInfFile_plainEmpty() {
    IProject project = testProjectCreator.getProject();

    IFile webXml = WebProjectUtil.findInWebInf(project, new Path("web.xml"));
    assertNull("Should not be found in empty plain project", webXml);
  }

  @Test
  public void testCreateWebInfFile_plainEmpty() throws CoreException, IOException {
    IProject project = testProjectCreator.getProject();
    // default webapp location is src/main/webapp
    IFile fooTxt =
        WebProjectUtil.createFileInWebInf(
            project, new Path("foo.txt"), asInputStream("foo"), false /*overwrite*/, null);
    assertNotNull(fooTxt);
    assertTrue(fooTxt.exists());
    assertEquals("src/main/webapp/WEB-INF/foo.txt", fooTxt.getProjectRelativePath().toString());
    assertEquals("foo", readAsString(fooTxt));
  }

  @Test
  public void testCreateWebInfFolder_plainEmpty() throws CoreException {
    IProject project = testProjectCreator.getProject();
    // default webapp location is src/main/webapp
    IFolder libFolder = WebProjectUtil.createFolderInWebInf(project, new Path("lib"), null);
    assertNotNull(libFolder);
    assertTrue(libFolder.exists());
    assertEquals("src/main/webapp/WEB-INF/lib", libFolder.getProjectRelativePath().toString());
  }


  @Test
  public void testFindWebInfFile_plainMavenLikeProject() throws CoreException {
    IProject project = testProjectCreator.getProject();
    createFile(project, new Path("src/main/webapp/WEB-INF/web.xml"), asInputStream("<web-app/>"));

    IFile webXml = WebProjectUtil.findInWebInf(project, new Path("web.xml"));
    assertNotNull(webXml);
    assertEquals("src/main/webapp/WEB-INF/web.xml", webXml.getProjectRelativePath().toString());
  }

  @Test
  public void testCreateWebInfFile_plainMavenLikeProject() throws CoreException, IOException {
    IProject project = testProjectCreator.getProject();
    IFile webXml = createFile(project, new Path("src/main/webapp/WEB-INF/web.xml"),
        asInputStream("<web-app/>"));
    assertNotNull(webXml);
    assertTrue(webXml.exists());

    // should be found alongside the web.xml
    IFile fooTxt =
        WebProjectUtil.createFileInWebInf(
            project, new Path("foo.txt"), asInputStream("foo"), false /*overwrite*/, null);
    assertNotNull(fooTxt);
    assertTrue(fooTxt.exists());
    assertEquals("src/main/webapp/WEB-INF/foo.txt", fooTxt.getProjectRelativePath().toString());
    assertEquals("foo", readAsString(fooTxt));
  }

  @Test
  public void testCreateWebInfFolder_plainMavenLikeProject() throws CoreException {
    IProject project = testProjectCreator.getProject();
    IFile webXml = createFile(project, new Path("src/main/webapp/WEB-INF/web.xml"),
        asInputStream("<web-app/>"));
    assertNotNull(webXml);
    assertTrue(webXml.exists());

    // should be found alongside the web.xml
    IFolder libFolder = WebProjectUtil.createFolderInWebInf(project, new Path("lib"), null);
    assertNotNull(libFolder);
    assertTrue(libFolder.exists());
    assertEquals("src/main/webapp/WEB-INF/lib", libFolder.getProjectRelativePath().toString());
  }


  @Test
  public void testFindWebInfFile_plainWtpLikeProject() throws CoreException {
    // WTP normally suggests putting the content in WebContent/
    IProject project = testProjectCreator.getProject();
    createFile(project, new Path("WebContent/WEB-INF/web.xml"), asInputStream("<web-app/>"));

    IFile webXml = WebProjectUtil.findInWebInf(project, new Path("web.xml"));
    assertNotNull(webXml);
    assertEquals("WebContent/WEB-INF/web.xml", webXml.getProjectRelativePath().toPortableString());
  }

  @Test
  public void testCreateWebInfFile_plainWtpLikeProject() throws CoreException, IOException {
    IProject project = testProjectCreator.getProject();
    IFile webXml =
        createFile(project, new Path("WebContent/WEB-INF/web.xml"), asInputStream("<web-app/>"));
    assertNotNull(webXml);
    assertTrue(webXml.exists());

    // should be found alongside the web.xml
    IFile fooTxt =
        WebProjectUtil.createFileInWebInf(
            project, new Path("foo.txt"), asInputStream("foo"), false /*overwrite*/, null);
    assertNotNull(fooTxt);
    assertTrue(fooTxt.exists());
    assertEquals("WebContent/WEB-INF/foo.txt", fooTxt.getProjectRelativePath().toString());
    assertEquals("foo", readAsString(fooTxt));
  }

  @Test
  public void testCreateWebInfFolder_plainWtpLikeProject() throws CoreException {
    IProject project = testProjectCreator.getProject();
    IFile webXml =
        createFile(project, new Path("WebContent/WEB-INF/web.xml"), asInputStream("<web-app/>"));
    assertNotNull(webXml);
    assertTrue(webXml.exists());

    // should be found alongside the web.xml
    IFolder libFolder = WebProjectUtil.createFolderInWebInf(project, new Path("lib"), null);
    assertNotNull(libFolder);
    assertTrue(libFolder.exists());
    assertEquals("WebContent/WEB-INF/lib", libFolder.getProjectRelativePath().toString());
  }


  @Test
  public void testFindWebInfFile_plainWebProject() throws CoreException {
    // WTP normally suggests putting the content in WebContent/
    IProject project = testProjectCreator.getProject();
    createFile(project, new Path("web/WEB-INF/web.xml"), asInputStream("<web-app/>"));

    IFile webXml = WebProjectUtil.findInWebInf(project, new Path("web.xml"));
    assertNotNull(webXml);
    assertEquals("web/WEB-INF/web.xml", webXml.getProjectRelativePath().toPortableString());
  }

  @Test
  public void testCreateWebInfFile_plainWebProject() throws CoreException, IOException {
    IProject project = testProjectCreator.getProject();
    IFile webXml =
        createFile(project, new Path("web/WEB-INF/web.xml"), asInputStream("<web-app/>"));
    assertNotNull(webXml);
    assertTrue(webXml.exists());

    // should be found alongside the web.xml
    IFile fooTxt =
        WebProjectUtil.createFileInWebInf(
            project, new Path("foo.txt"), asInputStream("foo"), false /*overwrite*/, null);
    assertNotNull(fooTxt);
    assertTrue(fooTxt.exists());
    assertEquals("web/WEB-INF/foo.txt", fooTxt.getProjectRelativePath().toString());
    assertEquals("foo", readAsString(fooTxt));
  }

  @Test
  public void testCreateWebInfFolder_plainWebProject() throws CoreException {
    IProject project = testProjectCreator.getProject();
    IFile webXml =
        createFile(project, new Path("web/WEB-INF/web.xml"), asInputStream("<web-app/>"));
    assertNotNull(webXml);
    assertTrue(webXml.exists());

    // should be found alongside the web.xml
    IFolder libFolder = WebProjectUtil.createFolderInWebInf(project, new Path("lib"), null);
    assertNotNull(libFolder);
    assertTrue(libFolder.exists());
    assertEquals("web/WEB-INF/lib", libFolder.getProjectRelativePath().toString());
  }


  @Test
  public void testFindWebInfFile_dynamicWebProject() {
    // WTP's Dynamic Web Project should create a web.xml
    IProject project = testProjectCreator
        .withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25).getProject();

    IFile webXml = WebProjectUtil.findInWebInf(project, new Path("web.xml"));
    assertNotNull(webXml);
    assertTrue(webXml.exists());
  }

  @Test
  public void testCreateWebInfFile_dynamicWebProject() throws CoreException, IOException {
    IProject project = testProjectCreator
        .withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25).getProject();
    IFile webXml = WebProjectUtil.findInWebInf(project, new Path("web.xml"));
    assertNotNull(webXml);
    assertTrue(webXml.exists());
    IFolder webInfDir = (IFolder) webXml.getParent();

    // should be found alongside the web.xml
    IFile fooTxt =
        WebProjectUtil.createFileInWebInf(
            project, new Path("foo.txt"), asInputStream("foo"), false /*overwrite*/, null);
    assertNotNull(fooTxt);
    assertTrue(fooTxt.exists());
    assertEquals(webInfDir.getProjectRelativePath().append("foo.txt").toString(),
        fooTxt.getProjectRelativePath().toString());
    assertEquals("foo", readAsString(fooTxt));
  }

  @Test
  public void testCreateWebInfFolder_dynamicWebProject() throws CoreException {
    IProject project = testProjectCreator
        .withFacets(JavaFacet.VERSION_1_7, WebFacetUtils.WEB_25).getProject();
    IFile webXml = WebProjectUtil.findInWebInf(project, new Path("web.xml"));
    assertNotNull(webXml);
    assertTrue(webXml.exists());
    IFolder webInfDir = (IFolder) webXml.getParent();

    // should be found alongside the web.xml
    IFolder libFolder = WebProjectUtil.createFolderInWebInf(project, new Path("lib"), null);
    assertNotNull(libFolder);
    assertTrue(libFolder.exists());
    assertEquals(webInfDir.getProjectRelativePath().append("lib").toString(),
        libFolder.getProjectRelativePath().toString());
  }


  @Test
  public void testFindWebInfFile_dynamicWebProject_defaultRootSourceProject()
      throws CoreException, IOException {
    // WTP's Dynamic Web Project with multiple <wb-resource> elements
    // - looking up a file should look in the wb-resource in order
    Map<String, IProject> projects =
        ProjectUtils.importProjects(
            getClass(), "projects/test-dynamic-web-project-dynamicrootsource.zip", true, null);
    assertEquals(1, projects.size());
    importedProject = projects.values().iterator().next();

    createFile(importedProject, new Path("target/m2e-wtp/web-resources/WEB-INF/foo.txt"),
        asInputStream("m2e-wtp"));
    createFile(importedProject, new Path("src/main/webapp/WEB-INF/foo.txt"),
        asInputStream("webapp"));
    createFile(importedProject, new Path("src/main/webapp/WEB-INF/bar.txt"),
        asInputStream("webapp"));

    // foo.txt should be resolved to the first <wb-resource>
    IFile fooTxt = WebProjectUtil.findInWebInf(importedProject, new Path("foo.txt"));
    assertNotNull(fooTxt);
    assertTrue(fooTxt.exists());
    assertEquals("target/m2e-wtp/web-resources/WEB-INF/foo.txt",
        fooTxt.getProjectRelativePath().toString());
    assertEquals("m2e-wtp", readAsString(fooTxt));

    // bar.txt should be resolved to the second <wb-resource>
    IFile barTxt = WebProjectUtil.findInWebInf(importedProject, new Path("bar.txt"));
    assertNotNull(barTxt);
    assertTrue(barTxt.exists());
    assertEquals("src/main/webapp/WEB-INF/bar.txt", barTxt.getProjectRelativePath().toString());
    assertEquals("webapp", readAsString(barTxt));
  }

  @Test
  public void testCreateWebInfFile_dynamicWebProject_defaultRootSourceProject()
      throws CoreException, IOException {
    // WTP's Dynamic Web Project with multiple <wb-resource> elements
    // - creating a file should be put in the defaultRootSource
    Map<String, IProject> projects =
        ProjectUtils.importProjects(
            getClass(), "projects/test-dynamic-web-project-dynamicrootsource.zip", true, null);
    assertEquals(1, projects.size());
    importedProject = projects.values().iterator().next();

    IFile fooTxt =
        WebProjectUtil.createFileInWebInf(
            importedProject, new Path("foo.txt"), asInputStream("foo"), false /*overwrite*/, null);

    // foo.txt should be created in the <wb-resource> tagged with `defaultRootSource`
    assertNotNull(fooTxt);
    assertTrue(fooTxt.exists());
    assertEquals("src/main/webapp/WEB-INF/foo.txt", fooTxt.getProjectRelativePath().toString());
    assertEquals("foo", readAsString(fooTxt));
  }

  @Test
  public void testCreateWebInfFolder_dynamicWebProject_defaultRootSourceProject()
      throws CoreException, IOException {
    // WTP's Dynamic Web Project with multiple <wb-resource> elements
    // - creating a file should be put in the defaultRootSource
    Map<String, IProject> projects =
        ProjectUtils.importProjects(
            getClass(), "projects/test-dynamic-web-project-dynamicrootsource.zip", true, null);
    assertEquals(1, projects.size());
    importedProject = projects.values().iterator().next();
    IFolder libFolder = WebProjectUtil.createFolderInWebInf(importedProject, new Path("lib"), null);

    // lib should be created in the <wb-resource> tagged with `defaultRootSource`
    assertNotNull(libFolder);
    assertTrue(libFolder.exists());
    assertEquals("src/main/webapp/WEB-INF/lib", libFolder.getProjectRelativePath().toString());
  }

  @Test
  public void testHasJsps_noFiles() throws CoreException {
    IVirtualFolder root = mock(IVirtualFolder.class, "/");
    IVirtualFile txt = mock(IVirtualFile.class, "/a.txt");
    when(txt.getFileExtension()).thenReturn("txt");
    IVirtualFolder folder = mock(IVirtualFolder.class, "/a");
    when(root.members()).thenReturn(new IVirtualResource[] {txt, folder});
    when(folder.members()).thenReturn(new IVirtualResource[] {});

    assertFalse(WebProjectUtil.hasJsps(root));
    // no jsps, should traverse all folders
    verify(root, times(2)).members();
    verify(folder, times(2)).members();
    verify(txt).getFileExtension();
    verifyNoMoreInteractions(root, folder, txt);
  }

  @Test
  public void testHasJsps_jspInRoot() throws CoreException {
    IVirtualFolder root = mock(IVirtualFolder.class, "/");
    IVirtualFile jsp = mock(IVirtualFile.class, "/a.jsp");
    when(jsp.getFileExtension()).thenReturn("jsp");
    IVirtualFolder folder = mock(IVirtualFolder.class, "/a");
    when(root.members()).thenReturn(new IVirtualResource[] {jsp, folder});
    when(folder.members()).thenReturn(new IVirtualResource[] {});

    assertTrue(WebProjectUtil.hasJsps(root));
    // jsp in root means no traversal to folder
    verify(root).members();
    verify(jsp).getFileExtension();
    verify(folder, times(0)).members();
    verifyNoMoreInteractions(root, folder, jsp);
  }

  @Test
  public void testHasJsps_jspInSub() throws CoreException {
    IVirtualFolder root = mock(IVirtualFolder.class, "/");
    IVirtualFolder folder = mock(IVirtualFolder.class, "/a");
    IVirtualFile jsp = mock(IVirtualFile.class, "/a/a.jsp");
    when(jsp.getFileExtension()).thenReturn("jsp");
    when(root.members()).thenReturn(new IVirtualResource[] {folder});
    when(folder.members()).thenReturn(new IVirtualResource[] {jsp});

    assertTrue(WebProjectUtil.hasJsps(root));
    verify(root, times(2)).members();
    verify(folder).members();
    verify(jsp).getFileExtension();
    verifyNoMoreInteractions(root, folder, jsp);
  }

  /** Create a file at the specific location, ensuring all folders are created as required. */
  private IFile createFile(IProject project, Path filePath, InputStream fileContents)
      throws CoreException {
    IFile file = project.getFile(filePath);
    ResourceUtils.createFolders(file.getParent(), null);
    assertTrue(file.getParent().exists());
    if (file.exists()) {
      file.setContents(fileContents, IFile.FORCE, null);
    } else {
      file.create(fileContents, IFile.FORCE, null);
    }
    return file;
  }

  /**
   * Convert the content into a ByteArrayInputStream.
   */
  private static InputStream asInputStream(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Read the contents of the provided file as a UTF-8-encoded string.
   */
  private Object readAsString(IFile file) throws IOException, CoreException {
    try (InputStream input = file.getContents()) {
      return new String(ByteStreams.toByteArray(input), StandardCharsets.UTF_8);
    }
  }
}
