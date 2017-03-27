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

package com.google.cloud.tools.eclipse.dataflow.core.preferences;

import com.google.common.collect.Lists;

import org.eclipse.core.resources.IProject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link DataflowPreferences} hierarchy. {@code ChainedDataflowPreferences} returns the earliest
 * available value for preferences from the preference stores it is constructed from. {@code
 * ChainedDataflowPreferences} does not support modification of the contained preferences.
 */
public class ProjectOrWorkspaceDataflowPreferences implements DataflowPreferences {
  private final List<DataflowPreferences> preferenceChain;

  /**
   * Returns the {@link DataflowPreferences} for the given project, with project and then workspace
   * preferences.
   */
  public static ProjectOrWorkspaceDataflowPreferences forProject(IProject project) {
    return new ProjectOrWorkspaceDataflowPreferences(
        WritableDataflowPreferences.forProject(project), WritableDataflowPreferences.global());
  }

  /**
   * Returns the {@link DataflowPreferences} for the workspace.
   */
  public static DataflowPreferences forWorkspace() {
    return new ProjectOrWorkspaceDataflowPreferences(WritableDataflowPreferences.global());
  }

  private ProjectOrWorkspaceDataflowPreferences(DataflowPreferences... preferences) {
    preferenceChain = Arrays.asList(preferences);
  }

  @Override
  public String getDefaultProject() {
    for (DataflowPreferences subPref : preferenceChain) {
      String defaultProject = subPref.getDefaultProject();
      if (defaultProject != null) {
        return defaultProject;
      }
    }
    return null;
  }

  @Override
  public String getDefaultStagingLocation() {
    for (DataflowPreferences subPref : preferenceChain) {
      String defaultStagingLocation = subPref.getDefaultStagingLocation();
      if (defaultStagingLocation != null) {
        return defaultStagingLocation;
      }
    }
    return null;
  }

  @Override
  public String getDefaultGcpTempLocation() {
    for (DataflowPreferences subPref : preferenceChain) {
      String defaultGcpTempLocation = subPref.getDefaultGcpTempLocation();
      if (defaultGcpTempLocation != null) {
        return defaultGcpTempLocation;
      }
    }
    return null;
  }

  @Override
  public Map<String, String> asDefaultPropertyMap() {
    Map<String, String> result = new HashMap<>();
    for (DataflowPreferences preferences : Lists.reverse(preferenceChain)) {
      result.putAll(preferences.asDefaultPropertyMap());
    }
    return result;
  }
}

