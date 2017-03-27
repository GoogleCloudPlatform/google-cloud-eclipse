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

package com.google.cloud.tools.eclipse.appengine.libraries;

import com.google.common.base.Preconditions;
import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

public class LibraryClasspathContainer implements IClasspathContainer {

  private final IPath containerPath;
  private final String description;
  private final List<IClasspathEntry> classpathEntries;

  public LibraryClasspathContainer(IPath path, String description,
      List<IClasspathEntry> classpathEntries) {
    Preconditions.checkNotNull(path, "path is null");
    Preconditions.checkNotNull(description, "description is null");
    Preconditions.checkArgument(!description.isEmpty(), "description is empty");
    Preconditions.checkNotNull(classpathEntries, "classpathEntries is null");

    this.containerPath = path;
    this.description = description;
    this.classpathEntries = classpathEntries;
  }

  /**
   * Creates a new {@link LibraryClasspathContainer} with the same path and description,
   * but with the <code>classpathEntries</code>.
   *
   * @param classpathEntries the classpath entries of the new container
   */
  public LibraryClasspathContainer copyWithNewEntries(List<IClasspathEntry> classpathEntries) {
    return new LibraryClasspathContainer(containerPath, description, classpathEntries);
  }

  @Override
  public IPath getPath() {
    return containerPath;
  }

  @Override
  public int getKind() {
    return IClasspathContainer.K_APPLICATION;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public IClasspathEntry[] getClasspathEntries() {
    return classpathEntries.toArray(new IClasspathEntry[0]);
  }
}
