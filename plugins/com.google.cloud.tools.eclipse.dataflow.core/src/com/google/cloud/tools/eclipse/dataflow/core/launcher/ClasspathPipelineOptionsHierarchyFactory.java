/*
 * Copyright 2015 Google Inc.
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

package com.google.cloud.tools.eclipse.dataflow.core.launcher;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.cloud.tools.eclipse.dataflow.core.DataflowCorePlugin;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.JavaProjectPipelineOptionsHierarchy;
import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsHierarchy;
import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

/**
 * A Factory that creates instances of {@link PipelineOptionsHierarchy} based on the classpath of
 * the project, or an {@link EmptyPipelineOptionsHierarchy} if no project is provided.
 */
public class ClasspathPipelineOptionsHierarchyFactory implements PipelineOptionsHierarchyFactory {
  /**
   * Returns an {@link EmptyPipelineOptionsHierarchy}.
   */
  @Override
  public PipelineOptionsHierarchy global(IProgressMonitor monitor) {
    return new EmptyPipelineOptionsHierarchy();
  }

  @Override
  public PipelineOptionsHierarchy forProject(
      IProject project, MajorVersion version, IProgressMonitor monitor)
      throws PipelineOptionsRetrievalException {
    IJavaElement javaProject = (IJavaElement) project.getAdapter(IJavaElement.class);
    checkNotNull(
        javaProject,
        "%s cannot be created for a non-java project: %s",
        JavaProjectPipelineOptionsHierarchy.class.getSimpleName(),
        project);
    try {
      return new JavaProjectPipelineOptionsHierarchy(
          javaProject.getJavaProject(), version, monitor);
    } catch (JavaModelException e) {
      DataflowCorePlugin.logError(e,
          "Error while constructing Pipeline Options Hierarchy for project %s", project.getName());
      throw new PipelineOptionsRetrievalException(e);
    }
  }
}
