/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.eclipse.appengine.facets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;

/**
 * Utility classes for processing WTP Web Projects (jst.web and jst.utility).
 */
public class WebProjectUtil {
  final static String DEFAULT_WEB_PATH = "src/main/webapp";

  final static String WEB_INF = "WEB-INF/";

  /**
   * Return the project's <code>WEB-INF</code> directory. There is no guarantee that the contents
   * are actually published.
   * 
   * @return the <code>IFolder</code> or null if not present
   */
  public static IFolder getWebInfDirectory(IProject project) {
    // Try to obtain the directory as if it was a Dynamic Web Project
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component != null && component.exists()) {
      IVirtualFolder root = component.getRootFolder();
      // the root should exist, but the WEB-INF may not yet exist
      if (root.exists()) {
        return (IFolder) root.getFolder(WEB_INF).getUnderlyingFolder();
      }
    }
    // Otherwise it's seemingly fair game
    IFolder defaultLocation = project.getFolder(DEFAULT_WEB_PATH).getFolder(WEB_INF);
    if (defaultLocation.exists()) {
      return defaultLocation;
    } else {
      return null;
    }
  }

  /**
   * Attempt to resolve the given file within the project's <code>WEB-INF</code>.
   * 
   * @return the file location or {@code null} if not found
   */
  public static IFile findInWebInf(IProject project, IPath filePath) {
    IFolder webInfFolder = getWebInfDirectory(project);
    if (webInfFolder == null) {
      return null;
    }
    IFile file = webInfFolder.getFile(filePath);
    return file.exists() ? file : null;
  }

  /**
   * Return the set of Java source paths for the given project, relative to the project. Return an
   * empty list if not a Java project.
   */
  public static List<IPath> getJavaSourcePaths(IProject project) {
    IJavaProject javaProject = JavaCore.create(project);
    if (!javaProject.exists()) {
      return Collections.emptyList();
    }
    try {
      IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
      List<IPath> paths = new ArrayList<>();
      for (IClasspathEntry entry : classpathEntries) {
        if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
          // source paths are absolute to the root folder of the project
          paths.add(entry.getPath().removeFirstSegments(1));
        }
      }
      return paths;
    } catch (JavaModelException ex) {
      return Collections.emptyList();
    }
  }

}
