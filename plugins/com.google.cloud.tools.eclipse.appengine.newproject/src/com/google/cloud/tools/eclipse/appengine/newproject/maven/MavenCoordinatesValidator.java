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

package com.google.cloud.tools.eclipse.appengine.newproject.maven;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;

/**
 * Responsible for validating Maven coordinate data.
 */
public class MavenCoordinatesValidator {
  /**
   * Validate the provided group identifier.
   */
  public static boolean validateGroupId(String groupId) {
    return validateMavenId(groupId);
  }

  /**
   * Validate the provided artifact identifier.
   */
  public static boolean validateArtifactId(String artifactId) {
    return validateMavenId(artifactId);
  }

  /** Validate the provided Maven version string. */
  public static boolean validateVersion(String version) {
    // Maven surprisingly doesn't perform validation on version strings
    return version != null && !version.isEmpty();
  }

  private static boolean validateMavenId(String id) {
    // somewhat loosely cribbed from MavenProjectWizardPage
    if (id == null) {
      return false;
    }
    // verify is a valid project name
    IStatus nameStatus = getWorkspace().validateName(id, IResource.PROJECT);
    if (!nameStatus.isOK()) {
      return false;
    }
    // verify matches pattern
    return id.matches("[A-Za-z0-9_\\-.]+"); //$NON-NLS-1$
  }

  private static IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }
}
