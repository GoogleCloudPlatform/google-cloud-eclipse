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

package com.google.cloud.tools.eclipse.appengine.newproject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import com.google.cloud.tools.eclipse.util.MavenUtils;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

public abstract class CreateAppEngineJarProjectTest extends CreateAppEngineProjectTest {

  @Override
  protected String getMostImportantFilename() {
    return "HelloAppEngineMain.java";
  }

  @Test
  public void testMavenNatureEnabled() throws InvocationTargetException, CoreException {
    config.setUseMaven("my.group.id", "my-artifact-id", "12.34.56");

    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);
    ProjectUtils.waitForProjects(project);

    assertTrue(project.hasNature(MavenUtils.MAVEN2_NATURE_ID));
    assertFalse(project.getFolder("build").exists());
    assertOutputDirectory("target/classes");
  }
}
