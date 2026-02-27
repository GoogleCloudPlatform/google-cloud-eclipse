/*
 * Copyright 2018 Google LLC
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.junit.Rule;
import org.junit.Test;

public class AppEngineConfigurationUtilTest {
  @Rule
  public TestProjectCreator projectCreator =
      new TestProjectCreator().withFacets(WebFacetUtils.WEB_31, JavaFacet.VERSION_1_8);

  @Test
  public void testCreateConfigurationFile_byteContent() throws CoreException, IOException {
    IFile file =
        AppEngineConfigurationUtil.createConfigurationFile(
            projectCreator.getProject(),
            new Path("example.txt"),
            new ByteArrayInputStream(new byte[] {0, 1, 2, 3}),
            true,
            null);
    try (InputStream input = file.getContents()) {
      byte[] contents = ByteStreams.toByteArray(input);
      assertArrayEquals(new byte[] {0, 1, 2, 3}, contents);
    }
  }

  @Test
  public void testCreateConfigurationFile_charContent() throws CoreException, IOException {
    String original = "\u1f64c this is a test \u00b5";
    IFile file =
        AppEngineConfigurationUtil.createConfigurationFile(
            projectCreator.getProject(), new Path("example.txt"), original, true, null);
    try (InputStream input = file.getContents()) {
      String contents = new String(ByteStreams.toByteArray(input), StandardCharsets.UTF_8);
      assertEquals(original, contents);
    }
  }

  @Test
  public void testCreateConfigurationFile_noAppEngineDir() throws CoreException, IOException {
    IFile file =
        AppEngineConfigurationUtil.createConfigurationFile(
            projectCreator.getProject(),
            new Path("index.yaml"),
            ByteSource.empty().openStream(),
            true,
            null);
    assertEquals(new Path("WebContent/WEB-INF/index.yaml"), file.getProjectRelativePath());
  }

  @Test
  public void testCreateConfigurationFile_withAppEngineDir() throws CoreException, IOException {
    IProject project = projectCreator.getProject();
    IFolder srcMainAppengine = project.getFolder(new Path("src/main/appengine"));
    ResourceUtils.createFolders(srcMainAppengine, null);
    IFile file =
        AppEngineConfigurationUtil.createConfigurationFile(
            project, new Path("index.yaml"), ByteSource.empty().openStream(), true, null);
    assertEquals(new Path("src/main/appengine/index.yaml"), file.getProjectRelativePath());
  }

  @Test
  public void testFindConfigurationFile_noAppEngineDir() throws CoreException, IOException {
    // ensure the configured WEB-INF directory (WebContent) is found
    IProject project = projectCreator.getProject();
    assertTrue(project.getFolder("WebContent/WEB-INF").exists());
    ResourceUtils.createFolders(project.getFolder("src/main/appengine"), null);

    IFile original = project.getFile(new Path("WebContent/WEB-INF/index.yaml"));
    original.create(ByteSource.empty().openStream(), true, null);

    IFile resolved =
        AppEngineConfigurationUtil.findConfigurationFile(project, new Path("index.yaml"));
    assertEquals(original, resolved);
  }

  /** Ensure the file found in src/main/appengine wins. */
  @Test
  public void testFindConfigurationFile_withAppEngineDir() throws CoreException, IOException {
    IProject project = projectCreator.getProject();
    assertTrue(project.getFolder("WebContent/WEB-INF").exists());
    ResourceUtils.createFolders(project.getFolder("src/main/appengine"), null);

    project
        .getFile("WebContent/WEB-INF/index.yaml")
        .create(ByteSource.empty().openStream(), true, null);
    IFile original = project.getFile("src/main/appengine/index.yaml");
    original.create(ByteSource.empty().openStream(), true, null);

    IFile resolved =
        AppEngineConfigurationUtil.findConfigurationFile(project, new Path("index.yaml"));
    assertEquals(original, resolved);
  }
}
