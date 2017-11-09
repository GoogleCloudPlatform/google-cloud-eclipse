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

package com.google.cloud.tools.eclipse.util.io;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Provides {@link IPath} related utility methods
 */
public class PathUtil {

  private PathUtil() {
  }

  /**
   * Makes a relative path absolute based off of another path. If the initial path is not relative, than it will be
   * returned without changes.
   *
   * @param path the relative path
   * @param basePath the base path to which the relative path is appended 
   * @return the path appended to the base path if the path is relative, otherwise the original path
   */
  public static IPath makePathAbsolute(IPath path, IPath basePath) {
    if (path == null) {
      return basePath;
    }
    if (path.isAbsolute() || basePath == null) {
      return path;
    } else {
      return basePath.append(path);
    }
  }

  /**
   * Returns the relative path of the original <code>path</code> with respect to the <code>basePath</code> if they share
   * the same prefix path, otherwise <code>path</code> is returned unchanged. If <code>path</code> is <code>null</code>,
   * <code>basePath</code> is returned. If <code>basePath</code> is <code>null</code>, <code>path</code> is returned.
   */
  public static IPath relativizePath(IPath path, IPath basePath) {
    return relativizePath(path, basePath, false);
  }

  /**
   * Returns the relative path of the original <code>path</code> with respect to the <code>basePath</code> if they share
   * the same prefix path, otherwise <code>null</code>. If <code>path</code> is <code>null</code>,
   * <code>basePath</code> is returned. If <code>basePath</code> is <code>null</code>, <code>path</code> is returned.
   */
  public static IPath relativizePathStrict(IPath path, IPath basePath) {
    return relativizePath(path, basePath, true);
  }
  
  private static IPath relativizePath(IPath path, IPath basePath, boolean strict) {
    if (path ==  null) {
      return basePath;
    }
    if (basePath == null) {
      return path;
    }
    java.nio.file.Path base = basePath.toFile().toPath().toAbsolutePath();
    java.nio.file.Path child = path.toFile().toPath().toAbsolutePath();
    if (child.startsWith(base)) {
      return new Path(base.relativize(child).toString());
    } else if (strict) {
      return null;
    } else {
      return path;
    }
  }
}
