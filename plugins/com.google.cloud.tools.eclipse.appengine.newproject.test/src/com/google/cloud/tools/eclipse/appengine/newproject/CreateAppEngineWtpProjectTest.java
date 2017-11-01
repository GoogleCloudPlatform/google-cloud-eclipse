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

public abstract class CreateAppEngineWtpProjectTest extends CreateAppEngineProjectTest {

  @Override
  protected String getMostImportantFilename() {
    return "HelloAppEngine.java";
  }

  @Test
  public void testFaviconAdded() throws InvocationTargetException, CoreException {
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);
    assertTrue("favicon.ico not found", project.getFile("src/main/webapp/favicon.ico").exists());
  }

  @Test
  public void testNoMavenNatureByDefault() throws InvocationTargetException, CoreException {
    assertFalse(config.getUseMaven());
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);

    assertFalse(project.hasNature(MavenUtils.MAVEN2_NATURE_ID));
    assertTrue(project.getFolder("build").exists());
    assertOutputDirectory("build/classes");
  }

  @Test
  public void testMavenNatureEnabled() throws InvocationTargetException, CoreException {
    config.setUseMaven("my.group.id", "my-artifact-id", "12.34.56");

    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);
    ProjectUtils.waitForProjects(project);

    assertTrue(project.hasNature(MavenUtils.MAVEN2_NATURE_ID));
    assertFalse(project.getFolder("build").exists());
    assertOutputDirectory("target/my-artifact-id-12.34.56/WEB-INF/classes");
  }

  @Test
  public void testJUnit4ClasspathIfNotUsingMaven() throws InvocationTargetException, CoreException {
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);
    assertTrue(hasJUnit4Classpath(project));
  }

  @Test
  public void testJstl12JarIfNonMavenProject() throws InvocationTargetException, CoreException {
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);

    assertTrue(project.getFile("src/main/webapp/WEB-INF/lib/fake-jstl-jstl-1.2.jar").exists());
  }

  @Test
  public void testNoJstl12JarIfMavenProject() throws InvocationTargetException, CoreException {
    config.setUseMaven("my.group.id", "my-other-artifact-id", "12.34.56");
    CreateAppEngineProject creator = newCreateAppEngineProject();
    creator.execute(monitor);

    assertFalse(project.getFile("src/main/webapp/WEB-INF/lib/fake-jstl-jstl-1.2.jar").exists());
  }
}
