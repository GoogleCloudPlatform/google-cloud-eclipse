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
public class DeployArtifactValidatorTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock private IObservableValue deployArtifactPath;

  private IPath basePath;
  private DeployArtifactValidator pathValidator;

  @Before
  public void setUp() throws IOException {
    basePath = new Path(tempFolder.newFolder().toString());
    when(deployArtifactPath.getValueType()).thenReturn(String.class);
    pathValidator = new DeployArtifactValidator(basePath, deployArtifactPath);
    assertTrue(basePath.isAbsolute());
  }

  @Test
  public void testConstructor_nonAbsoluteBasePath() {
    try {
      when(deployArtifactPath.getValue()).thenReturn("some.jar");
      new DeployArtifactValidator(new Path("non/absolute/base/path"), deployArtifactPath);
      fail();
    } catch (IllegalArgumentException ex) {
      assertEquals("basePath is not absolute.", ex.getMessage());
    }
  }

  @Test
  public void testValidate_emptyField() {
    when(deployArtifactPath.getValue()).thenReturn("");

    IStatus result = pathValidator.validate();
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("Missing WAR or JAR path.", result.getMessage());
  }

  @Test
  public void testValidate_relativePathAndNoArtifact() {
    when(deployArtifactPath.getValue()).thenReturn("relative/path/some.jar");

    IStatus result = pathValidator.validate();
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("File does not exist: "
        + new Path(basePath + "/relative/path/some.jar").toOSString(), result.getMessage());
  }

  @Test
  public void testValidate_absolutePathAndNoArtifact() {
    String absolutePath = basePath + "/sub/directory/some.jar";
    when(deployArtifactPath.getValue()).thenReturn(absolutePath);

    IStatus result = pathValidator.validate();
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("File does not exist: " + new Path(absolutePath).toOSString(),
        result.getMessage());
  }

  @Test
  public void testValidate_relativePathAndInvalidExtension() {
    when(deployArtifactPath.getValue()).thenReturn("relative/path/python.py");

    IStatus result = pathValidator.validate();
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("File extension is not \"war\" or \"jar\".", result.getMessage());
  }

  @Test
  public void testValidate_absolutePathInvalidExtension() {
    String absolutePath = basePath + "/sub/directory/python.py";
    when(deployArtifactPath.getValue()).thenReturn(absolutePath);

    IStatus result = pathValidator.validate();
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("File extension is not \"war\" or \"jar\".", result.getMessage());
  }

  @Test
  public void testValidate_relativePathNotFile() {
    basePath.append("some.war").toFile().mkdir();
    when(deployArtifactPath.getValue()).thenReturn("some.war");

    IStatus result = pathValidator.validate();
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("Path is a directory: " + new Path(basePath + "/some.war").toOSString(),
        result.getMessage());
  }

  @Test
  public void testValidate_absolutePathNotFile() {
    basePath.append("some.war").toFile().mkdir();

    String absolutePath = basePath + "/some.war";
    when(deployArtifactPath.getValue()).thenReturn(absolutePath);

    IStatus result = pathValidator.validate();
    assertEquals(IStatus.ERROR, result.getSeverity());
    assertEquals("Path is a directory: " + new Path(basePath + "/some.war").toOSString(),
        result.getMessage());
  }

  @Test
  public void testValidate_relativePathWithJar() throws IOException {
    assertTrue(basePath.append("/some/directory/").toFile().mkdirs());
    assertTrue(basePath.append("/some/directory/some.jar").toFile().createNewFile());

    when(deployArtifactPath.getValue()).thenReturn("some/directory/some.jar");
    IStatus result = pathValidator.validate();
    assertTrue(result.isOK());
  }

  @Test
  public void testValidate_absolutePathWithJar() throws IOException {
    File absolutePath = tempFolder.newFolder("another", "folder");
    File jar = new File(absolutePath + "/some.jar");
    assertTrue(jar.createNewFile());

    when(deployArtifactPath.getValue()).thenReturn(jar.toString());
    IStatus result = pathValidator.validate();
    assertTrue(result.isOK());
  }
}
