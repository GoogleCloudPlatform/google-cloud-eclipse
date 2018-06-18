/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.eclipse.dataflow.core.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.eclipse.util.MavenCoordinatesValidator;
import com.google.cloud.tools.eclipse.util.status.StatusUtil;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import org.apache.maven.archetype.catalog.Archetype;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.junit.Assert;
import org.junit.Test;

public class DataflowProjectCreatorTest {

  @Test
  public void testCreate() {
    Assert.assertNotNull(DataflowProjectCreator.create());
  }
  
  @Test
  public void testTemplates() {
    for (DataflowProjectArchetype template : DataflowProjectArchetype.values()) {
      Assert.assertTrue(MavenCoordinatesValidator.validateArtifactId(template.getArtifactId()));
      Assert.assertFalse(template.getSdkVersions().isEmpty());
      Assert.assertFalse(template.getLabel().isEmpty());
    }
  }

  @Test
  public void testRun_propagatesCoreException()
      throws OperationCanceledException, InterruptedException, CoreException {
    IProjectConfigurationManager manager = mock(IProjectConfigurationManager.class);
    CoreException exception = new CoreException(StatusUtil.error(this, "test error message"));
    doThrow(exception).when(manager).createArchetypeProjects(
        any(IPath.class), any(Archetype.class), anyString(), anyString(), anyString(), anyString(),
        any(Properties.class), any(ProjectImportConfiguration.class), any(IProgressMonitor.class));

    DataflowProjectCreator creator = new DataflowProjectCreator(manager);
    creator.setMavenGroupId("com.example");
    creator.setMavenArtifactId("some-artifact-id");
    creator.setPackage("com.example");
    creator.setArchetypeVersion("123.456.789");
    try {
      creator.run(new NullProgressMonitor());
      fail();
    } catch (InvocationTargetException ex) {
      assertEquals(exception, ex.getCause());
    }
  }
}
