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

package com.google.cloud.tools.eclipse.dataflow.core.internal.properties;

import com.google.cloud.tools.eclipse.dataflow.core.project.DataflowDependencyManager;
import com.google.common.annotations.VisibleForTesting;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Tests the Dataflow Version of the project a resource is contained within.
 */
public class DataflowVersionTester extends PropertyTester {
  @VisibleForTesting
  static final String TRACKS_DATAFLOW_VERSION_PROPERTY = "tracksDataflowVersion";

  @VisibleForTesting
  static final String PINNED_DATAFLOW_VERSION_PROPERTY = "pinnedDataflowVersion";

  private DataflowDependencyManager dependencyManager;

  public DataflowVersionTester() {
    this(DataflowDependencyManager.create());
  }

  @VisibleForTesting
  DataflowVersionTester(DataflowDependencyManager dependencyManager) {
    this.dependencyManager = dependencyManager;
  }

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    switch (property) {
      case TRACKS_DATAFLOW_VERSION_PROPERTY:
        return testTracksDataflowVersion(receiver);
      case PINNED_DATAFLOW_VERSION_PROPERTY:
        return testPinnedDataflowVersion(receiver);
      default:
        return false;
    }
  }

  private boolean testPinnedDataflowVersion(Object receiver) {
    IProject project = getProjectForReciever(receiver);
    return project != null && dependencyManager.hasPinnedDataflowDependency(project);
  }

  private boolean testTracksDataflowVersion(Object receiver) {
    IProject project = getProjectForReciever(receiver);
    return project != null && dependencyManager.hasTrackedDataflowDependency(project);
  }

  private IProject getProjectForReciever(Object receiver) {
    if (!(receiver instanceof IAdaptable)) {
      return null;
    }
    IAdaptable adaptable = (IAdaptable) receiver;
    IResource resource = adaptable.getAdapter(IResource.class);
    if (resource != null) {
      return resource.getProject();
    }
    return null;
  }
}

