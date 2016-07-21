package com.google.cloud.tools.eclipse.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class MavenUtils {

  public static final String MAVEN2_NATURE_ID = "org.eclipse.m2e.core.maven2Nature";

  private static final Logger logger = Logger.getLogger(MavenUtils.class.getName());
  
  /**
   * Returns {@code true} if the given project has the Maven 2 nature. This
   * checks for the Maven nature used by m2Eclipse 1.0.0.
   */
  public static boolean hasMavenNature(IProject project) {
    try {
      if (NatureUtils.hasNature(project, MavenUtils.MAVEN2_NATURE_ID)) {
        return true;
      }
    } catch (CoreException coreException) {
      logger.log(Level.SEVERE, "Unable to examine natures on project " + project.getName(), coreException);
    }
    return false;
  }
}
