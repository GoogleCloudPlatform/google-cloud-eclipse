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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AppEngineDirectoryValidatorTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private IPath basePath;

  @Before
  public void setUp() throws IOException {
    basePath = new Path(tempFolder.newFolder().toString());
    assertTrue(basePath.isAbsolute());
  }

  @Test
  public void testContructor_nonAbsoluteBasePath() {
    try {
      new AppEngineDirectoryValidator(new Path("non/absolute/path"));
      fail();
    } catch (IllegalArgumentException ex) {}
  }

  @Test
  public void testValidate_relativePathAndNoAppYaml() {
    IStatus result = new AppEngineDirectoryValidator(basePath).validate("relative/path");
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("app.yaml does not exist in the App Engine directory: "
        + basePath + "/relative/path", result.getMessage());
  }

  @Test
  public void testValidate_absoluatePathAndNoAppYaml() {
    IStatus result = new AppEngineDirectoryValidator(basePath).validate("/absolute/path");
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("app.yaml does not exist in the App Engine directory: /absolute/path",
        result.getMessage());
  }

  @Test
  public void testValidate_relativePathWithAppYaml() throws IOException {
    new File(basePath + "/some/directory").mkdirs();
    File appYaml = Files.createFile(Paths.get(basePath + "/some/directory/app.yaml")).toFile();
    assertTrue(appYaml.exists());

    IStatus result = new AppEngineDirectoryValidator(basePath).validate("some/directory");
    assertTrue(result.isOK());
  }

  @Test
  public void testValidate_absolutePathWithAppYaml() throws IOException {
    File absolutePath = tempFolder.newFolder("another", "folder");
    File appYaml = Files.createFile(absolutePath.toPath().resolve("app.yaml")).toFile();
    assertTrue(appYaml.exists());

    IStatus result = new AppEngineDirectoryValidator(basePath).validate(absolutePath.toString());
    assertTrue(result.isOK());
  }
}
