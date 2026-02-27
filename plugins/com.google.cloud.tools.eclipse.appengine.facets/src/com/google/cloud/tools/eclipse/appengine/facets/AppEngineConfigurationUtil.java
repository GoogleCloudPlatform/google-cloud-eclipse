/*
 * Copyright 2018 Google LLC
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

import com.google.cloud.tools.eclipse.util.io.ResourceUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

/**
 * Utility class for resolving App Engine configuration files in a project. The goal of this class
 * is to insulate callers from having to know how and when configuration files are placed within a
 * project. For example, Java projects for the flexible environment typically place their YAML
 * configuration files in {@code src/main/appengine} whereas Java projects for the standard
 * environment place their XML configuration files within the {@code WEB-INF} directory.
 *
 * @see WebProjectUtil
 */
public class AppEngineConfigurationUtil {
  /** A well-known location for App Engine configuration files. */
  private static final IPath DEFAULT_CONFIGURATION_FILE_LOCATION = new Path("src/main/appengine");

  /**
   * Create an App Engine configuration file in the appropriate location for the project.
   *
   * @param project the hosting project
   * @param relativePath the path of the file relative to the configuration location
   * @param contents the content for the file
   * @param overwrite if {@code true} then overwrite the file if it exists
   */
  public static IFile createConfigurationFile(
      IProject project,
      IPath relativePath,
      String contents,
      boolean overwrite,
      IProgressMonitor monitor)
      throws CoreException {
    InputStream bytes = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
    return createConfigurationFile(project, relativePath, bytes, overwrite, monitor);
  }

  /**
   * Create an App Engine configuration file in the appropriate location for the project.
   *
   * @param project the hosting project
   * @param relativePath the path of the file relative to the configuration location
   * @param contents the content for the file
   * @param overwrite if {@code true} then overwrite the file if it exists
   */
  public static IFile createConfigurationFile(
      IProject project,
      IPath relativePath,
      InputStream contents,
      boolean overwrite,
      IProgressMonitor monitor)
      throws CoreException {
    IFolder appengineFolder = project.getFolder(DEFAULT_CONFIGURATION_FILE_LOCATION);
    if (appengineFolder != null && appengineFolder.exists()) {
      IFile destination = appengineFolder.getFile(relativePath);
      if (!destination.exists()) {
        IContainer parent = destination.getParent();
        ResourceUtils.createFolders(parent, monitor);
        destination.create(contents, true, monitor);
      } else if (overwrite) {
        destination.setContents(contents, IResource.FORCE, monitor);
      }
      return destination;
    }
    return WebProjectUtil.createFileInWebInf(project, relativePath, contents, overwrite, monitor);
  }

  /**
   * Attempt to resolve the given file within the project's {@code WEB-INF}. Note that this method
   * may return a file that is in a build location (e.g., {@code
   * target/m2e-wtp/web-resources/WEB-INF}) which may be frequently removed or regenerated.
   *
   * @return the file location or {@code null} if not found
   */
  public static IFile findConfigurationFile(IProject project, IPath relativePath) {
    IFolder appengineFolder = project.getFolder(DEFAULT_CONFIGURATION_FILE_LOCATION);
    if (appengineFolder != null && appengineFolder.exists()) {
      IFile destination = appengineFolder.getFile(relativePath);
      if (destination.exists()) {
        return destination;
      }
    }
    return WebProjectUtil.findInWebInf(project, relativePath);
  }

  private AppEngineConfigurationUtil() {} // not intended to be instantiated
}
