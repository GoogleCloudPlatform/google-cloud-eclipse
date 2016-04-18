package com.google.cloud.tools.eclipse.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class NatureUtils {

  /**
   * Returns {@code true} if the project is accessible and has the
   * specified nature ID.
   * 
   * @param project
   * @param natureId
   * 
   * @return {@code true} if the project is accessible and has the
   *         specified nature ID
   * @throws CoreException
   */
  public static boolean hasNature(IProject project, String natureId) throws CoreException {
    return project.isAccessible() && project.hasNature(natureId);
  }
}
