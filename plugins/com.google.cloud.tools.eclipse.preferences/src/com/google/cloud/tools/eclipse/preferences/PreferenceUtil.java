/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class PreferenceUtil {

  private static final String DEPLOY = "com.google.cloud.tools.eclipse.appengine.deploy";

  public static void setProjectIdPreference(IProject project, String projectId) {
    if (projectId != null && !projectId.isEmpty()) {
      IEclipsePreferences preferences = new ProjectScope(project).getNode(DEPLOY);
      preferences.put("project.id", projectId);
    }
  }

}
