
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
    return id.matches("[A-Za-z0-9_\\-.]+");
  }

  private static IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }
}
