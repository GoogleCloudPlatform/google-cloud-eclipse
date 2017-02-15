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

package com.google.cloud.tools.eclipse.projectselector;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.api.client.auth.oauth2.Credential;

public class ProjectRepositoryTest {

  @Test
  public void testGetProjects_nullCredential() throws ProjectRepositoryException {
    ProjectRepository repository = new ProjectRepository();
    Credential credential = null;
    List<GcpProject> projects = repository.getProjects(credential);
    Assert.assertTrue(projects.isEmpty());
  }

  @Test
  public void testGetProjects_credentialWithoutAccess() {
    ProjectRepository repository = new ProjectRepository();
    Credential credential = Mockito.mock(Credential.class);
    try {
      repository.getProjects(credential);
      Assert.fail();
    } catch (ProjectRepositoryException ex) {
      Assert.assertTrue(ex.getMessage().contains("401"));
    }
  }
  
  @Test
  public void testConvertToGcpProjects_null() {
    List<GcpProject> projects = ProjectRepository.convertToGcpProjects(null);
    Assert.assertTrue(projects.isEmpty());
  }

}
