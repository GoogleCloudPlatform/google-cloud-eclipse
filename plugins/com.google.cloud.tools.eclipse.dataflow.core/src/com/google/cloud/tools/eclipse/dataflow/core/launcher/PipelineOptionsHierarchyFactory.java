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

import com.google.cloud.tools.eclipse.dataflow.core.launcher.options.PipelineOptionsHierarchy;
import com.google.cloud.tools.eclipse.dataflow.core.project.MajorVersion;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A factory that constructs instances of {@link PipelineOptionsHierarchy}.
 */
public interface PipelineOptionsHierarchyFactory {
  /**
   * Construct a PipelineOptionsHeirarchy that acquires workspace options.
   */
  PipelineOptionsHierarchy global(IProgressMonitor monitor);

  /**
   * Construct a {@link PipelineOptionsHierarchy} that acquires pipeline options within the scope of
   * the provided project. The provided {@link IProject} cannot be null.
   */
  PipelineOptionsHierarchy forProject(
      IProject project, MajorVersion projectVersion, IProgressMonitor monitor)
      throws PipelineOptionsRetrievalException;
}
