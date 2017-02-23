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

package com.google.cloud.tools.eclipse.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;

public class NatureUtils {

  /**
   * Returns {@code true} if the project is accessible and has the
   * specified nature ID.
   *
   * @return {@code true} if the project is accessible and has the
   *         specified nature ID
   */
  public static boolean hasNature(IProject project, String natureId) throws CoreException {
    return project.isAccessible() && project.hasNature(natureId);
  }

  /**
   * Removes nature identified by {@code natureId}. If the {@code project} does not have the
   * nature, this method does nothing.
   */
  public static void removeNature(IProject project, String natureId) throws CoreException {
    if (project.hasNature(natureId)) {
      // Remove the nature ID from the natures in the project description
      IProjectDescription description = project.getDescription();
      List<String> natures = new ArrayList<>(Arrays.asList(description.getNatureIds()));
      natures.remove(natureId);
      description.setNatureIds(natures.toArray(new String[0]));
      project.setDescription(description, null /* monitor */);
    }
  }
}
