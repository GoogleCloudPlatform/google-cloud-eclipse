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

package com.google.cloud.tools.eclipse.appengine.deploy.ui.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AppYamlPathValidatorTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock private IObservableValue appYamlPath;

  private IPath basePath;
  private AppYamlPathValidator pathValidator;

  @Before
  public void setUp() throws IOException {
    basePath = new Path(tempFolder.newFolder().toString());
    when(appYamlPath.getValueType()).thenReturn(String.class);
    pathValidator = new AppYamlPathValidator(basePath, appYamlPath);
    assertTrue(basePath.isAbsolute());
  }

  @Test
  public void testContructor_nonAbsoluteBasePath() {
    try {
      when(appYamlPath.getValue()).thenReturn("app.yaml");
      new AppYamlPathValidator(new Path("non/absolute/base/path"), appYamlPath);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("basePath is not absolute.", ex.getMessage());
    }
  }

  @Test
  public void testValidate_relativePathAndNoAppYaml() {
    when(appYamlPath.getValue()).thenReturn("relative/path/app.yaml");

    IStatus result = pathValidator.validate();
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("app.yaml does not exist.", result.getMessage());
  }

  @Test
  public void testValidate_absolutePathAndNoAppYaml() {
    String absolutePath = basePath + "/sub/directory/app.yaml";
    when(appYamlPath.getValue()).thenReturn(absolutePath);

    IStatus result = pathValidator.validate();
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("app.yaml does not exist.", result.getMessage());
  }

  @Test
  public void testValidate_relativePathAndInvalidFileName() {
    when(appYamlPath.getValue()).thenReturn("relative/path/my-app.yaml");

    IStatus result = pathValidator.validate();
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("File name is not app.yaml: "
        + new Path(basePath + "/relative/path/my-app.yaml").toOSString(),
        result.getMessage());
  }

  @Test
  public void testValidate_absolutePathInvalidFileName() {
    String absolutePath = basePath + "/sub/directory/my-app.yaml";
    when(appYamlPath.getValue()).thenReturn(absolutePath);

    IStatus result = pathValidator.validate();
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("File name is not app.yaml: "
        + new Path(basePath + "/sub/directory/my-app.yaml").toOSString(),
        result.getMessage());
  }

  @Test
  public void testValidate_relativePathNotFile() {
    createAppYamlAsDirectory(basePath);
    when(appYamlPath.getValue()).thenReturn("app.yaml");

    IStatus result = pathValidator.validate();
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("Not a file: " + new Path(basePath + "/app.yaml").toOSString(),
        result.getMessage());
  }

  @Test
  public void testValidate_absolutePathNotFile() {
    createAppYamlAsDirectory(basePath);

    String absolutePath = basePath + "/app.yaml";
    when(appYamlPath.getValue()).thenReturn(absolutePath);

    IStatus result = pathValidator.validate();
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("Not a file: " + new Path(basePath + "/app.yaml").toOSString(),
        result.getMessage());
  }

  @Test
  public void testValidate_relativePathWithAppYaml() throws IOException {
    new File(basePath + "/some/directory").mkdirs();
    File appYaml = Files.createFile(Paths.get(basePath + "/some/directory/app.yaml")).toFile();
    assertTrue(appYaml.exists());

    when(appYamlPath.getValue()).thenReturn("some/directory/app.yaml");
    IStatus result = pathValidator.validate();
    assertTrue(result.isOK());
  }

  @Test
  public void testValidate_absolutePathWithAppYaml() throws IOException {
    File absolutePath = tempFolder.newFolder("another", "folder");
    File appYaml = Files.createFile(absolutePath.toPath().resolve("app.yaml")).toFile();
    assertTrue(appYaml.exists());

    when(appYamlPath.getValue()).thenReturn(appYaml.toString());
    IStatus result = pathValidator.validate();
    assertTrue(result.isOK());
  }

  private static void createAppYamlAsDirectory(IPath basePath) {
    File appYamlAsDirectory = basePath.append("app.yaml").toFile();
    appYamlAsDirectory.mkdir();
    assertTrue(appYamlAsDirectory.isDirectory());
  }
}
