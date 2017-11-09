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

package com.google.cloud.tools.eclipse.dataflow.core.launcher;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsHierarchy;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsNamespaces;
import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ClasspathPipelineOptionsHierarchyFactoryTest {

  @Mock private IProject project;
  @Mock private IJavaProject javaProject;

  @Before
  public void setUp() {
    IJavaElement javaElement = mock(IJavaElement.class);
    when(project.getAdapter(IJavaElement.class)).thenReturn(javaElement);
    when(javaElement.getJavaProject()).thenReturn(javaProject);
  }

  @Test
  public void testForProject_projectHasNoPipelineOptionsType()
      throws PipelineOptionsRetrievalException {
    PipelineOptionsHierarchy optionsHeierarchy = new ClasspathPipelineOptionsHierarchyFactory()
        .forProject(project, null, new NullProgressMonitor());
    assertThat(optionsHeierarchy, instanceOf(EmptyPipelineOptionsHierarchy.class));
  }

  @Test
  public void testForProject_pipelineOptionsTypeDoesNotExistInProject()
      throws PipelineOptionsRetrievalException, JavaModelException {
    String version = PipelineOptionsNamespaces.rootType(MajorVersion.ONE);
    when(javaProject.findType(version)).thenReturn(mock(IType.class));

    PipelineOptionsHierarchy optionsHeierarchy = new ClasspathPipelineOptionsHierarchyFactory()
        .forProject(project, MajorVersion.ONE, new NullProgressMonitor());
    assertThat(optionsHeierarchy, instanceOf(EmptyPipelineOptionsHierarchy.class));
  }
}
