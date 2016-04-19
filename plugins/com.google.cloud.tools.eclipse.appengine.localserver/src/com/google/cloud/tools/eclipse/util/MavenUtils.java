package com.google.cloud.tools.eclipse.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.google.cloud.tools.eclipse.appengine.localserver.Activator;

public class MavenUtils {

  public static final String MAVEN2_NATURE_ID = "org.eclipse.m2e.core.maven2Nature";

  /**
   * Returns {@code true} if the given project has the Maven 2 nature. This
   * checks for the Maven nature used by m2Eclipse 1.0.0.
   */
  public static boolean hasMavenNature(IProject project) {
    try {
      if (NatureUtils.hasNature(project, MavenUtils.MAVEN2_NATURE_ID)) {
        return true;
      }
    } catch (CoreException ce) {
      Activator.getDefault()
               .getLog()
               .log(new Status(IStatus.ERROR,
                               Activator.PLUGIN_ID,
                               "Unable to examine natures on project " + project.getName(),
                               ce));
    }
    return false;
  }
}
