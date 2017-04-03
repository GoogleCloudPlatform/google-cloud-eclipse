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

package com.google.cloud.tools.eclipse.appengine.newproject.flex;

import static org.mockito.Mockito.mock;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.repository.ILibraryRepositoryService;
import com.google.cloud.tools.eclipse.appengine.newproject.AppEngineProjectConfig;
import com.google.cloud.tools.eclipse.appengine.newproject.CreateAppEngineWtpProject;
import com.google.cloud.tools.eclipse.test.util.project.ProjectUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Test;

public class CreateAppEngineStandardWtpProjectTest {

  private NullProgressMonitor monitor = new NullProgressMonitor();
  private AppEngineProjectConfig config = new AppEngineProjectConfig();
  private IProject project;

  @Before
  public void setUp() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    project = workspace.getRoot().getProject("testproject" + Math.random());
    config.setProject(project);
  }

  @Test
  public void testAppEngineLibrariesAdded() throws InvocationTargetException, CoreException {
    ILibraryRepositoryService repositoryService = mock(ILibraryRepositoryService.class);

    Library library = new Library("javax.servlet-api");
    config.setAppEngineLibraries(Collections.singletonList(library));
    CreateAppEngineWtpProject creator =
        new CreateAppEngineFlexWtpProject(config, mock(IAdaptable.class), repositoryService);
    creator.execute(new NullProgressMonitor());

    ProjectUtils.waitForProjects(project); // App Engine runtime is added via a Job, so wait.
    //assertAppEngineContainerOnClasspath(library);
  }

}
