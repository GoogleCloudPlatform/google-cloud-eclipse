/*
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
 */

package com.google.cloud.tools.eclipse.appengine.newproject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.ui.AppEngineRuntime;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import org.eclipse.core.resources.IProject;
import org.junit.Assert;
import org.junit.Test;

public class AppEngineProjectConfigTest {

  private AppEngineProjectConfig config = new AppEngineProjectConfig();

  @Test
  public void testProject() {
    IProject project = mock(IProject.class);
    config.setProject(project);
    assertSame(project, config.getProject());
  }

  @Test
  public void testPackageName() {
    config.setPackageName("com.foo.bar");
    Assert.assertEquals("com.foo.bar", config.getPackageName());
  }

  @Test
  public void testServiceName() {
    Assert.assertNull(config.getServiceName());
    config.setServiceName("foobar");
    Assert.assertEquals("foobar", config.getServiceName());
  }

  @Test
  public void testRuntime() {
    Assert.assertNull(config.getRuntime());
    config.setRuntime(AppEngineRuntime.STANDARD_JAVA_8);
    Assert.assertEquals(AppEngineRuntime.STANDARD_JAVA_8, config.getRuntime());

    config.setRuntime(AppEngineRuntime.STANDARD_JAVA_8_SERVLET_25);
    Assert.assertEquals(AppEngineRuntime.STANDARD_JAVA_8_SERVLET_25, config.getRuntime());
  }

  @Test
  public void testEclipseProjectLocationUri() throws URISyntaxException {
    config.setEclipseProjectLocationUri(new URI("file://foo/bar"));
    Assert.assertEquals(new URI("file://foo/bar"), config.getEclipseProjectLocationUri());
  }

  @Test
  public void testAppEngineLibraries() {
    config.setLibraries(Collections.singleton(new Library("app-engine-library")));
    assertThat(config.getLibraries().size(), is(1));
    assertThat(config.getLibraries().iterator().next().getId(), is("app-engine-library"));
  }

  @Test
  public void testGetUseMaven_defaultFalse() {
    assertFalse(config.getUseMaven());
  }

  @Test
  public void testSetUseMaven() {
    config.setUseMaven("group.foo", "artifact.bar", "version.baz");
    assertTrue(config.getUseMaven());
    assertEquals("group.foo", config.getMavenGroupId());
    assertEquals("artifact.bar", config.getMavenArtifactId());
    assertEquals("version.baz", config.getMavenVersion());
  }
  
  @Test
  public void testEntryPoint() {
    assertNull(config.getEntryPoint());
    config.setEntryPoint("java -Xmx64m -jar your-artifact.jar");
    assertEquals("java -Xmx64m -jar your-artifact.jar", config.getEntryPoint());
  }
}
