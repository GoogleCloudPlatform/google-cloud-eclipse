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

package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;;

@RunWith(MockitoJUnitRunner.class)
public class CreateMavenBasedAppEngineStandardProjectTest {

  @Mock
  private IProjectConfigurationManager manager;

  private NullProgressMonitor monitor = new NullProgressMonitor();

  @Test
  public void testConstructor()
      throws InvocationTargetException, CoreException, InterruptedException {
    CreateMavenBasedAppEngineStandardProject operation =
        new CreateMavenBasedAppEngineStandardProject();
    operation.setGroupId("group");
    operation.setArtifactId("artifact");

    operation.projectConfigurationManager = manager;

    operation.execute(monitor);
    ProjectUtils.waitUntilIdle();  // App Engine runtime is added via a Job, so wait.
  }

}
